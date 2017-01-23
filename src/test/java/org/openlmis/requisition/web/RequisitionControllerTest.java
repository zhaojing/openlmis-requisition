package org.openlmis.requisition.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.OrderableProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.StockAdjustmentReasonReferenceDataService;
import org.openlmis.requisition.validate.DraftRequisitionValidator;
import org.openlmis.requisition.validate.RequisitionValidator;
import org.openlmis.utils.FacilitySupportsProgramHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class RequisitionControllerTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private Requisition initiatedRequsition;

  @Mock
  private Requisition submittedRequsition;

  @Mock
  private Requisition authorizedRequsition;

  @Mock
  private Requisition approvedRequsition;

  @Mock
  private RequisitionTemplate template;

  @Mock
  private RequisitionValidator validator;

  @Mock
  private DraftRequisitionValidator draftValidator;

  @Mock
  private RequisitionTemplateRepository templateRepository;

  @Mock
  private StockAdjustmentReasonReferenceDataService stockAdjustmentReasonReferenceDataService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private RequisitionDtoBuilder requisitionDtoBuilder;

  @Mock
  private FacilitySupportsProgramHelper facilitySupportsProgramHelper;

  @Mock
  private OrderableProductReferenceDataService orderableProductReferenceDataService;

  @InjectMocks
  private RequisitionController requisitionController;

  private UUID programUuid = UUID.randomUUID();
  private UUID facilityUuid = UUID.randomUUID();

  private UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private UUID uuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private UUID uuid4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
  private UUID uuid5 = UUID.fromString("00000000-0000-0000-0000-000000000005");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    List<ProcessingPeriodDto> processingPeriods = generateProcessingPeriods();
    when(initiatedRequsition.getStatus()).thenReturn(RequisitionStatus.INITIATED);
    when(submittedRequsition.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.AUTHORIZED);
    when(approvedRequsition.getStatus()).thenReturn(RequisitionStatus.APPROVED);

    when(periodService.getPeriods(programUuid, facilityUuid, false))
        .thenReturn(processingPeriods);
    when(periodService.getPeriods(programUuid, facilityUuid, true))
        .thenReturn(Collections.singletonList(processingPeriods.get(0)));

    mockRequisitionRepository();
  }

  @Test
  public void shouldReturnCurrentPeriodForEmergency() throws Exception {
    ResponseEntity<?> response =
        requisitionController.getProcessingPeriodIds(programUuid, facilityUuid, true);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    List<ProcessingPeriodDto> periods = (List<ProcessingPeriodDto>) response.getBody();

    verify(periodService).getPeriods(programUuid, facilityUuid, true);
    verifyZeroInteractions(periodService, requisitionRepository);

    assertNotNull(periods);
    assertEquals(1, periods.size());

    List<UUID> periodUuids = periods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertTrue(periodUuids.contains(uuid1));
  }

  @Test
  public void shouldSubmitValidInitiatedRequisition() {
    when(initiatedRequsition.getTemplate()).thenReturn(template);
    when(requisitionRepository.findOne(uuid1)).thenReturn(initiatedRequsition);

    requisitionController.submitRequisition(uuid1);

    verify(initiatedRequsition).submit(Collections.emptyList());
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(any(Requisition.class), anyList());
  }

  @Test
  public void shouldNotSubmitInvalidRequisition() {
    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject("requisitionLineItems",
          "approvedQuantity is only available during the approval step of the requisition process");
      return null;
    }).when(validator).validate(eq(initiatedRequsition), any(Errors.class));
    when(initiatedRequsition.getId()).thenReturn(uuid1);

    requisitionController.submitRequisition(uuid1);

    verifyNoSubmitOrUpdate(initiatedRequsition);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldReturnBadRequestWhenRequisitionIdDiffersFromTheOneInUrl() throws Exception {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);
    when(requisitionDto.getId()).thenReturn(uuid1);
    when(requisitionDto.getTemplate()).thenReturn(null);
    when(requisitionDto.getFacility()).thenReturn(mock(FacilityDto.class));
    when(requisitionDto.getProgram()).thenReturn(mock(ProgramDto.class));
    when(requisitionDto.getProcessingPeriod()).thenReturn(mock(ProcessingPeriodDto.class));
    when(initiatedRequsition.getTemplate()).thenReturn(template);
    when(requisitionRepository.findOne(uuid2)).thenReturn(initiatedRequsition);

    requisitionController.updateRequisition(requisitionDto, uuid2);

  }

  @Test
  public void shouldUpdateRequisition() throws Exception {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);

    when(requisitionDto.getId()).thenReturn(uuid1);
    when(requisitionDto.getFacility()).thenReturn(mock(FacilityDto.class));
    when(requisitionDto.getProgram()).thenReturn(mock(ProgramDto.class));
    when(requisitionDto.getProcessingPeriod()).thenReturn(mock(ProcessingPeriodDto.class));
    when(requisitionDto.getSupervisoryNode()).thenReturn(UUID.randomUUID());

    when(initiatedRequsition.getTemplate()).thenReturn(template);
    when(initiatedRequsition.getSupervisoryNodeId()).thenReturn(null);
    when(initiatedRequsition.getId()).thenReturn(uuid1);

    ResponseEntity responseEntity = requisitionController.updateRequisition(requisitionDto, uuid1);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(template, initiatedRequsition.getTemplate());

    verify(initiatedRequsition).updateFrom(any(Requisition.class), anyList());
    verify(requisitionRepository).save(initiatedRequsition);
    verify(stockAdjustmentReasonReferenceDataService)
        .getStockAdjustmentReasonsByProgram(any(UUID.class));
    verifySupervisoryNodeWasNotUpdated(initiatedRequsition);
  }

  @Test
  public void shouldNotUpdateWithInvalidRequisition() {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);
    when(requisitionDto.getTemplate()).thenReturn(template);
    when(requisitionDto.getFacility()).thenReturn(mock(FacilityDto.class));
    when(requisitionDto.getProgram()).thenReturn(mock(ProgramDto.class));
    when(requisitionDto.getProcessingPeriod()).thenReturn(mock(ProcessingPeriodDto.class));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject("requisitionLineItems[0].beginningBalance", "Bad argument");

      return null;
    }).when(draftValidator).validate(any(Requisition.class), any(Errors.class));

    requisitionController.updateRequisition(requisitionDto, uuid1);

    verifyNoSubmitOrUpdate(initiatedRequsition);
  }

  @Test
  public void shouldThrowExceptionWhenFacilityOrProgramIdNotFound() throws Exception {
    exception.expect(ValidationMessageException.class);
    requisitionController.initiate(programUuid, null, null, false);
    exception.expect(ValidationMessageException.class);
    requisitionController.initiate(null, facilityUuid, null, false);
  }

  private List<ProcessingPeriodDto> generateProcessingPeriods() {
    ProcessingPeriodDto period = new ProcessingPeriodDto();
    period.setId(uuid1);
    ProcessingPeriodDto period2 = new ProcessingPeriodDto();
    period2.setId(uuid2);
    ProcessingPeriodDto period3 = new ProcessingPeriodDto();
    period3.setId(uuid3);
    ProcessingPeriodDto period4 = new ProcessingPeriodDto();
    period4.setId(uuid4);
    ProcessingPeriodDto period5 = new ProcessingPeriodDto();
    period5.setId(uuid5);

    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(period);
    periods.add(period2);
    periods.add(period3);
    periods.add(period4);
    periods.add(period5);

    return periods;
  }

  private void mockRequisitionRepository() {
    when(requisitionRepository.searchByProcessingPeriodAndType(uuid1, false))
        .thenReturn(new ArrayList<>());
    when(requisitionRepository.searchByProcessingPeriodAndType(uuid2, false))
        .thenReturn(Arrays.asList(initiatedRequsition));
    when(requisitionRepository.searchByProcessingPeriodAndType(uuid3, false))
        .thenReturn(Arrays.asList(submittedRequsition));
    when(requisitionRepository.searchByProcessingPeriodAndType(uuid4, false))
        .thenReturn(Arrays.asList(authorizedRequsition));
    when(requisitionRepository.searchByProcessingPeriodAndType(uuid5, false))
        .thenReturn(Arrays.asList(approvedRequsition));
    when(requisitionRepository.save(initiatedRequsition))
        .thenReturn(initiatedRequsition);
    when(requisitionRepository.findOne(uuid1))
        .thenReturn(initiatedRequsition);
  }

  private void verifyNoSubmitOrUpdate(Requisition requisition) {
    verifyZeroInteractions(requisitionService);
    verify(requisition, never()).updateFrom(any(Requisition.class), anyList());
    verify(requisition, never()).submit(Collections.emptyList());
  }

  private void verifySupervisoryNodeWasNotUpdated(Requisition requisition) {
    verify(requisition, never()).setSupervisoryNodeId(any());
    assertNull(requisition.getSupervisoryNodeId());
  }
}
