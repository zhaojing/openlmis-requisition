package org.openlmis.requisition.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.domain.Comment;
import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionLineItem;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.dto.CommentDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.service.RequisitionCommentService;
import org.openlmis.requisition.service.RequisitionLineCalculationService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RequisitionDtoBuilderTest {

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private RequisitionLineCalculationService requisitionLineCalculationService;

  @Mock
  private RequisitionCommentService requisitionCommentService;

  @InjectMocks
  private RequisitionDtoBuilder requisitionDtoBuilder = new RequisitionDtoBuilder();

  private Requisition requisition;

  @Mock
  private RequisitionLineItem requisitionLineItem;

  @Mock
  private Comment comment;

  @Mock
  private FacilityDto facilityDto;

  @Mock
  private ProcessingPeriodDto processingPeriodDto;

  @Mock
  private ProgramDto programDto;

  private List<RequisitionLineItemDto> lineItemDtos = new ArrayList<>();
  private List<CommentDto> commentDtos = new ArrayList<>();

  private UUID requisitionUuid = UUID.randomUUID();
  private UUID facilityUuid = UUID.randomUUID();
  private UUID processingPeriodUuid = UUID.randomUUID();
  private UUID programUuid = UUID.randomUUID();
  private UUID supervisoryNodeUuid = UUID.randomUUID();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(requisitionLineCalculationService.exportToDtos(anyListOf(RequisitionLineItem.class)))
        .thenReturn(lineItemDtos);
    when(requisitionCommentService.exportToDtos(anyListOf(Comment.class))).thenReturn(commentDtos);

    requisition = buildRequisition();
  }

  @Test
  public void shouldBuildDtoFromRequisition() {
    when(facilityReferenceDataService.findOne(facilityUuid)).thenReturn(facilityDto);
    when(programReferenceDataService.findOne(programUuid)).thenReturn(programDto);
    when(periodReferenceDataService.findOne(processingPeriodUuid)).thenReturn(processingPeriodDto);

    RequisitionDto requisitionDto = requisitionDtoBuilder.build(requisition);

    verify(requisitionLineCalculationService).exportToDtos(anyListOf(RequisitionLineItem.class));
    verify(requisitionCommentService).exportToDtos(anyListOf(Comment.class));

    assertNotNull(requisitionDto);
    assertEquals(requisition.getId(), requisitionDto.getId());
    assertEquals(requisition.getSupervisoryNodeId(), requisitionDto.getSupervisoryNode());
    assertEquals(requisition.getEmergency(), requisitionDto.getEmergency());
    assertEquals(facilityDto, requisitionDto.getFacility());
    assertEquals(programDto, requisitionDto.getProgram());
    assertEquals(processingPeriodDto, requisitionDto.getProcessingPeriod());
    assertEquals(lineItemDtos, requisitionDto.getRequisitionLineItems());
    assertEquals(commentDtos, requisitionDto.getComments());
  }

  @Test
  public void shouldBuildDtoFromRequisitionWhenReferenceDataInstancesDoNotExist() {
    when(facilityReferenceDataService.findOne(facilityUuid)).thenReturn(null);
    when(programReferenceDataService.findOne(programUuid)).thenReturn(null);
    when(periodReferenceDataService.findOne(processingPeriodUuid)).thenReturn(null);

    RequisitionDto requisitionDto = requisitionDtoBuilder.build(requisition);

    verify(requisitionLineCalculationService).exportToDtos(anyListOf(RequisitionLineItem.class));
    verify(requisitionCommentService).exportToDtos(anyListOf(Comment.class));

    assertNotNull(requisitionDto);
    assertNull(requisitionDto.getFacility());
    assertNull(requisitionDto.getProgram());
    assertNull(requisitionDto.getProcessingPeriod());
  }

  private Requisition buildRequisition() {
    Requisition requisition = new Requisition();
    requisition.setId(requisitionUuid);
    requisition.setFacilityId(facilityUuid);
    requisition.setProcessingPeriodId(processingPeriodUuid);
    requisition.setProgramId(programUuid);
    requisition.setSupervisoryNodeId(supervisoryNodeUuid);
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.setComments(Arrays.asList(comment));
    requisition.setRequisitionLineItems(Arrays.asList(requisitionLineItem));
    requisition.setEmergency(false);

    return requisition;
  }
}