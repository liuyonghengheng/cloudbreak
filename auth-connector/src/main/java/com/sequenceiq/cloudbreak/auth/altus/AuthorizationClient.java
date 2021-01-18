package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final Tracer tracer;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     * @param tracer   tracer
     */
    AuthorizationClient(ManagedChannel channel, String actorCrn, Tracer tracer) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
        this.tracer = tracer;
    }

    public void checkRight(String requestId, String userCrn, String right, String resource) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(right);
        AuthorizationProto.RightCheck.Builder rightCheckBuilder = AuthorizationProto.RightCheck.newBuilder().setRight(right);
        if (!StringUtils.isEmpty(resource)) {
            rightCheckBuilder.setResource(resource);
        }
        newStub(requestId).checkRight(
                AuthorizationProto.CheckRightRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .setCheck(rightCheckBuilder.build())
                        .build()
        );
    }

    public List<Boolean> hasRights(String requestId, String actorCrn, Iterable<AuthorizationProto.RightCheck> rightChecks) {
        checkNotNull(requestId);
        checkNotNull(actorCrn);
        checkNotNull(rightChecks);
        AuthorizationProto.HasRightsResponse response = newStub(requestId).hasRights(
                AuthorizationProto.HasRightsRequest.newBuilder()
                        .setActorCrn(actorCrn)
                        .addAllCheck(rightChecks)
                        .build()
        );
        return response.getResultList();
    }

    public List<Boolean> hasRightOnResources(String requestId, String actorCrn, String right, Iterable<AuthorizationProto.Resource> resources) {
        checkNotNull(requestId);
        checkNotNull(actorCrn);
        checkNotNull(resources);
        return newStub(requestId)
                .hasRightOnResources(
                        AuthorizationProto.HasRightOnResourcesRequest
                                .newBuilder()
                                .setActorCrn(actorCrn)
                                .setRight(right)
                                .addAllResource(resources)
                                .build())
                .getResultList();
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private AuthorizationGrpc.AuthorizationBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return AuthorizationGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTracingInterceptor(tracer),
                new AltusMetadataInterceptor(requestId, actorCrn)
        );
    }
}
