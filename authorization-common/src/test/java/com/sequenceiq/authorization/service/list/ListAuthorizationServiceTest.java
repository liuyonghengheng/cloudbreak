package com.sequenceiq.authorization.service.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationFiltering;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
public class ListAuthorizationServiceTest {

    private static final Optional<String> REQUEST_ID = Optional.of("REQUEST_ID");

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    private static final Crn USER_CRN = Crn
            .builder(CrnResourceDescriptor.USER)
            .setAccountId(ACCOUNT_ID)
            .setResource("RESOURCE_ID")
            .build();

    private static final String ENVIRONMENT_CRN = dataHubCrn("env-1");

    private static final String DATAHUB_CRN = environmentCrn("datahub-1");

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.DESCRIBE_DATAHUB;

    private static final String LEGACY_RIGHT = "LEGACY_RIGHT";

    @Spy
    private Map<Class<AuthorizationFiltering<?>>, AuthorizationFiltering<?>> listResourceProviders = new HashMap<>();

    @Mock
    private AuthorizationFiltering<?> authorizationFiltering;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private ListParamsUtil listParamsUtil;

    @InjectMocks
    private ListAuthorizationService underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    private FilterListBasedOnPermissions checkList;

    @BeforeEach
    //CHECKSTYLE:OFF
    public void setUp() throws Throwable {
        //CHECKSTYLE:ON
        listResourceProviders.put((Class<AuthorizationFiltering<?>>) authorizationFiltering.getClass(), authorizationFiltering);
        checkList = annotation((Class<AuthorizationFiltering<?>>) authorizationFiltering.getClass(), ACTION);
        lenient().doAnswer(invocation -> invocation.getArgument(0)).when(authorizationFiltering).filterByIds(anyList(), anyMap());
        lenient().doAnswer(i -> List.of(1000L)).when(authorizationFiltering).getAll(anyMap());
        lenient().doAnswer(i -> underTest.getResultAs()).when(proceedingJoinPoint).proceed();
        lenient().when(listParamsUtil.getFilterParams(any(), any())).thenReturn(Map.of());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testLegacyAuthorizationWhenHasRight() throws Throwable {
        //CHECKSTYLE:ON
        disableListFiltering();
        disableResourceBasedAuthorization();
        when(umsRightProvider.getLegacyRight(ACTION)).thenReturn(LEGACY_RIGHT);
        when(grpcUmsClient.checkAccountRightLegacy(USER_CRN.toString(), USER_CRN.toString(), LEGACY_RIGHT, REQUEST_ID))
                .thenReturn(true);

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnNotFilteredResult(result);
    }

    @Test
    public void testLegacyAuthorizationWhenHasNoRight() {
        disableListFiltering();
        disableResourceBasedAuthorization();
        when(umsRightProvider.getLegacyRight(ACTION)).thenReturn(LEGACY_RIGHT);
        when(grpcUmsClient.checkAccountRightLegacy(USER_CRN.toString(), USER_CRN.toString(), LEGACY_RIGHT, REQUEST_ID))
                .thenReturn(false);

        AccessDeniedException exception =
                assertThrows(AccessDeniedException.class,
                        () -> underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID));

        assertEquals("You have no right to perform LEGACY_RIGHT in account ACCOUNT_ID.", exception.getMessage());
        verifyNoInteractions(authorizationFiltering, proceedingJoinPoint);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testResourceAuthorizationWhenListFilteringDisabledAndHasRight() throws Throwable {
        //CHECKSTYLE:ON
        disableListFiltering();
        enableResourceBasedAuthorization();
        when(grpcUmsClient.checkAccountRight(USER_CRN.toString(), USER_CRN.toString(), ACTION.getRight(), REQUEST_ID))
                .thenReturn(true);

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnNotFilteredResult(result);
    }

    @Test
    public void testResourceAuthorizationWhenListFilteringDisabledAndHasNoRight() {
        disableListFiltering();
        enableResourceBasedAuthorization();
        when(grpcUmsClient.checkAccountRight(USER_CRN.toString(), USER_CRN.toString(), ACTION.getRight(), REQUEST_ID))
                .thenReturn(false);

        AccessDeniedException exception =
                assertThrows(AccessDeniedException.class,
                        () -> underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID));

        assertEquals("You have no right to perform datahub/describeDatahub in account ACCOUNT_ID.", exception.getMessage());
        verifyNoInteractions(authorizationFiltering, proceedingJoinPoint);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testWithEmptyResourceList() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources();

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyNoInteractions(grpcUmsClient);
        verifyHasRightOnAndProceedWith(List.of(), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRight() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources(new AuthorizationResource(1L, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN),
                ACTION.getRight(),
                REQUEST_ID)).thenReturn(List.of(true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasNoRight() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources(new AuthorizationResource(1L, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN),
                ACTION.getRight(),
                REQUEST_ID)).thenReturn(List.of(false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnParent() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources(new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN),
                ACTION.getRight(),
                REQUEST_ID)).thenReturn(List.of(true, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnResourceWithParant() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources(new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasNoRightOnResourceWithParent() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        givenResources(new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnResourcesParent() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, ENVIRONMENT_CRN),
                new AuthorizationResource(3L, datahubCrn3, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(true, false, false, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 2L, 3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnTwoResourceButNotOnItsParent() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, ENVIRONMENT_CRN),
                new AuthorizationResource(3L, datahubCrn3, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, true, false, true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasNoRightOnAnyResources() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, ENVIRONMENT_CRN),
                new AuthorizationResource(3L, datahubCrn3, ENVIRONMENT_CRN));
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, false, false, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnOneParentResource() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, environmentCrn2),
                new AuthorizationResource(3L, datahubCrn3, environmentCrn2)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, false, true, false, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(2L, 3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnAllParentResources() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, environmentCrn2),
                new AuthorizationResource(3L, datahubCrn3, environmentCrn2)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(true, false, true, false, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 2L, 3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnOneSubResource() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, environmentCrn2),
                new AuthorizationResource(3L, datahubCrn3, environmentCrn2)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, false, false, false, true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testHasRightOnAllSubResources() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, environmentCrn2),
                new AuthorizationResource(3L, datahubCrn3, environmentCrn2)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(false, true, false, true, true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 2L, 3L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testResourcesWithParentAndWithoutParent() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        String datahubCrn4 = dataHubCrn("datahub-4");
        String datahubCrn5 = dataHubCrn("datahub-5");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, ENVIRONMENT_CRN),
                new AuthorizationResource(3L, datahubCrn3),
                new AuthorizationResource(4L, datahubCrn4),
                new AuthorizationResource(5L, datahubCrn5)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3, datahubCrn4, datahubCrn5),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(true, false, false, true, false, true));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 2L, 3L, 5L), result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testFilteringWithUnorderedAuthorizationResourceList() throws Throwable {
        //CHECKSTYLE:ON
        enableListFiltering();
        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        String datahubCrn4 = dataHubCrn("datahub-4");
        String datahubCrn5 = dataHubCrn("datahub-5");
        givenResources(
                new AuthorizationResource(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new AuthorizationResource(2L, datahubCrn2, environmentCrn2),
                new AuthorizationResource(3L, datahubCrn3, ENVIRONMENT_CRN),
                new AuthorizationResource(4L, datahubCrn4, environmentCrn2),
                new AuthorizationResource(5L, datahubCrn5, ENVIRONMENT_CRN)
        );
        when(grpcUmsClient.hasRightsOnResources(
                USER_CRN.toString(),
                USER_CRN.toString(),
                List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn3, datahubCrn5, environmentCrn2, datahubCrn2, datahubCrn4),
                ACTION.getRight(),
                REQUEST_ID)
        ).thenReturn(List.of(true, false, false, false, false, true, false));

        Object result = underTest.filterList(checkList, USER_CRN, proceedingJoinPoint, methodSignature, REQUEST_ID);

        verifyHasRightOnAndProceedWith(List.of(1L, 3L, 5L, 2L), result);
    }

    private void enableListFiltering() {
        when(entitlementService.listFilteringEnabled(any())).thenReturn(true);
    }

    private void disableListFiltering() {
        when(entitlementService.listFilteringEnabled(any())).thenReturn(false);
    }

    private void enableResourceBasedAuthorization() {
        when(entitlementService.isAuthorizationEntitlementRegistered(any())).thenReturn(true);
    }

    private void disableResourceBasedAuthorization() {
        when(entitlementService.isAuthorizationEntitlementRegistered(any())).thenReturn(false);
    }

    private void givenResources(AuthorizationResource... authorizationResources) {
        when(authorizationFiltering.getAllResources(Map.of())).thenReturn(List.of(authorizationResources));
    }

    //CHECKSTYLE:OFF
    private void verifyHasRightOnAndProceedWith(List<Long> expectedIds, Object result) throws Throwable {
        //CHECKSTYLE:ON
        assertEquals(expectedIds, result);
        verify(authorizationFiltering).filterByIds(expectedIds, Map.of());
        verify(proceedingJoinPoint).proceed();
    }

    //CHECKSTYLE:OFF
    private void verifyHasRightOnNotFilteredResult(Object result) throws Throwable {
        //CHECKSTYLE:ON
        verify(authorizationFiltering).getAll(Map.of());
        verify(proceedingJoinPoint).proceed();
        assertEquals(List.of(1000L), result);
    }

    private static String dataHubCrn(String resourceId) {
        return Crn.builder(CrnResourceDescriptor.DATAHUB)
                .setAccountId(ACCOUNT_ID)
                .setResource(resourceId)
                .build()
                .toString();
    }

    private static String environmentCrn(String resourceId) {
        return Crn.builder(CrnResourceDescriptor.ENVIRONMENT)
                .setAccountId(ACCOUNT_ID)
                .setResource(resourceId)
                .build()
                .toString();
    }

    private FilterListBasedOnPermissions annotation(Class<AuthorizationFiltering<?>> providerClass, AuthorizationResourceAction action) {
        return new FilterListBasedOnPermissions() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FilterListBasedOnPermissions.class;
            }

            @Override
            public AuthorizationResourceAction action() {
                return action;
            }

            @Override
            public Class<? extends AuthorizationFiltering<?>> filter() {
                return providerClass;
            }
        };
    }
}