/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.requisition.domain.requisition;

import static java.util.Objects.isNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.ADJUSTED_CONSUMPTION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_FIELD_MUST_HAVE_VALUES;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_INITIATED_TO_BE_SUBMMITED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_SUBMITTED_TO_BE_AUTHORIZED;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.CurrencyConfig;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.BaseTimestampedEntity;
import org.openlmis.requisition.domain.OpenLmisNumberUtils;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.ProofOfDeliveryLineItemDto;
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequisitionHelper;
import org.openlmis.requisition.utils.RightName;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.util.CollectionUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@SuppressWarnings("PMD.TooManyMethods")
@Entity
@TypeName("Requisition")
@Table(name = "requisitions")
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Requisition extends BaseTimestampedEntity {

  private static final XLogger LOGGER = XLoggerFactory.getXLogger(Requisition.class);

  public static final String FACILITY_ID = "facilityId";
  public static final String PROGRAM_ID = "programId";
  public static final String PROCESSING_PERIOD_ID = "processingPeriodId";
  public static final String TOTAL_CONSUMED_QUANTITY = "totalConsumedQuantity";
  public static final String STOCK_ON_HAND = "stockOnHand";
  public static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  public static final String EMERGENCY_FIELD = "emergency";
  public static final String DATE_PHYSICAL_STOCK_COUNT_COMPLETED =
      "datePhysicalStockCountCompleted";
  public static final String REQUISITION_LINE_ITEMS = "requisitionLineItems";


  @OneToMany(
      mappedBy = "requisition",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  @Getter
  @Setter
  private List<RequisitionLineItem> requisitionLineItems;

  @Getter
  private String draftStatusMessage;

  @ManyToOne
  @JoinColumn(name = "templateId", nullable = false)
  @DiffIgnore
  @Getter
  @Setter
  private RequisitionTemplate template;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID facilityId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID programId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID processingPeriodId;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supplyingFacilityId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private RequisitionStatus status;

  @OneToMany(
      mappedBy = "requisition",
      cascade = CascadeType.ALL)
  @DiffIgnore
  @Getter
  @Setter
  private List<StatusChange> statusChanges = new ArrayList<>();

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean emergency;

  @Column(nullable = false)
  @Getter
  @Setter
  private Integer numberOfMonthsInPeriod;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supervisoryNodeId;

  @ManyToMany
  @JoinTable(name = "requisitions_previous_requisitions",
      joinColumns = {@JoinColumn(name = "requisitionId")},
      inverseJoinColumns = {@JoinColumn(name = "previousRequisitionId")})
  @DiffIgnore
  @Getter
  @Setter
  private List<Requisition> previousRequisitions;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = UUID.class)
  @Column(name = "value")
  @CollectionTable(
      name = "available_products",
      joinColumns = @JoinColumn(name = "requisitionId"))
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private Set<UUID> availableProducts;

  @Getter
  @Setter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "localDate",
          column = @Column(name = "datephysicalstockcountcompleted"))
      })
  private DatePhysicalStockCountCompleted datePhysicalStockCountCompleted;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @JoinColumn(name = "requisitionId")
  @DiffIgnore
  @Getter
  @Setter
  private List<StockAdjustmentReason> stockAdjustmentReasons = new ArrayList<>();

  @OneToMany(
      mappedBy = "requisition",
      cascade = CascadeType.ALL)
  @DiffIgnore
  @Getter
  private List<RequisitionPermissionString> permissionStrings = new ArrayList<>();

  /**
   * Constructor.
   *
   * @param facilityId         id of the Facility
   * @param programId          id of the Program
   * @param processingPeriodId id of the ProcessingPeriod
   * @param status             status of the Requisition
   * @param emergency          whether this Requisition is emergency
   */
  public Requisition(UUID facilityId, UUID programId, UUID processingPeriodId,
                     RequisitionStatus status, Boolean emergency) {
    this.facilityId = facilityId;
    this.programId = programId;
    this.processingPeriodId = processingPeriodId;
    this.status = status;
    this.emergency = emergency;
    permissionStrings.add(RequisitionPermissionString.newRequisitionPermissionString(this,
        RightName.REQUISITION_VIEW, facilityId, programId));
  }

  /**
   * Validates if requisition can be updated.
   */
  public ValidationResult validateCanBeUpdated(
      RequisitionValidationService validationService) {
    return validationService.validateRequisitionCanBeUpdated();
  }

  /**
   * Returns a set of all orderable IDs in this requisition.
   */
  public Set<UUID> getAllOrderableIds() {
    Set<UUID> orderableIds = Optional
        .ofNullable(requisitionLineItems)
        .orElse(Collections.emptyList())
        .stream()
        .map(RequisitionLineItem::getOrderableId)
        .collect(Collectors.toSet());

    Optional.ofNullable(availableProducts).ifPresent(orderableIds::addAll);

    return orderableIds;
  }

  /**
   * Copy values of attributes into new or updated Requisition.
   *
   * @param requisition            Requisition with new values.
   * @param products               Collection of orderables.
   */
  public void updateFrom(
      Requisition requisition, Collection<OrderableDto> products,
      boolean isDatePhysicalStockCountCompletedEnabled) {

    this.numberOfMonthsInPeriod = requisition.getNumberOfMonthsInPeriod();

    this.draftStatusMessage = requisition.draftStatusMessage;

    updateReqLines(requisition.getRequisitionLineItems());
    calculateAndValidateTemplateFields(this.template);
    updateTotalCostAndPacksToShip(products);

    if (isDatePhysicalStockCountCompletedEnabled) {
      setDatePhysicalStockCountCompleted(requisition.getDatePhysicalStockCountCompleted());
    }

    // do this manually here, since JPA won't catch updates to collections (line items)
    setModifiedDate(ZonedDateTime.now());
  }

  /**
   * Initiates the state of a requisition by creating line items based on products
   *
   * @param template             the requisition template for this requisition to use (based on
   *                             program)
   * @param fullSupplyProducts   the full supply products for this requisitions facility to build
   *                             requisition lines for
   * @param previousRequisitions the previous requisitions for this program/facility. Used for field
   *                             calculations and set previous adjusted consumptions. Pass empty
   *                             list if there are no previous requisitions.
   */
  public void initiate(RequisitionTemplate template,
                       Collection<ApprovedProductDto> fullSupplyProducts,
                       List<Requisition> previousRequisitions,
                       int numberOfPreviousPeriodsToAverage,
                       ProofOfDeliveryDto proofOfDelivery,
                       Map<UUID, Integer> idealStockAmounts,
                       UUID initiator,
                       Map<UUID, Integer> orderableSoh) {

    Profiler profiler = new Profiler("REQUISITION_INITIATE_ENTITY");
    profiler.setLogger(LOGGER);
    this.template = template;
    this.previousRequisitions = previousRequisitions;

    profiler.start("SET_LINE_ITEMS");
    initiateLineItems(fullSupplyProducts, idealStockAmounts, orderableSoh);

    profiler.start("GET_PREV_BEGINNING_BALANCE");
    List<RequisitionLineItem> nonSkippedFullSupplyItems = null;
    // Firstly, if we display the column ...
    // ... and if the previous requisition exists ...
    if (!previousRequisitions.isEmpty()
        && null != previousRequisitions.get(0)
        && template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)) {
      // .. for each line from the current requisition ...
      nonSkippedFullSupplyItems = getNonSkippedFullSupplyRequisitionLineItems();

      Map<UUID, RequisitionLineItem> productIdToPreviousLine = previousRequisitions.get(0)
          .getRequisitionLineItems().stream().collect(
              toMap(RequisitionLineItem::getOrderableId, identity(),
                  (item1, item2) -> item1));

      getNonSkippedFullSupplyRequisitionLineItems().forEach(currentLine -> {
        // ... we try to find line in the previous requisition for the same product ...
        RequisitionLineItem previousLine = productIdToPreviousLine.getOrDefault(
            currentLine.getOrderableId(), null);

        // ... and in the end we use it to calculate beginning balance in a new line.
        currentLine.setBeginningBalance(
            LineItemFieldsCalculator.calculateBeginningBalance(previousLine));
      });
    }

    // Secondly, if Proof Of Delivery exists and it is submitted ...
    profiler.start("SET_RECV_QTY");
    if (null != proofOfDelivery && proofOfDelivery.isSubmitted()) {
      // .. for each line from the current requisition ...
      if (nonSkippedFullSupplyItems == null) {
        nonSkippedFullSupplyItems = getNonSkippedFullSupplyRequisitionLineItems();
      }

      Map<UUID, ProofOfDeliveryLineItemDto> productIdToPodLine = proofOfDelivery
          .getLineItems()
          .stream()
          .filter(li -> null != li.getOrderable())
          .collect(toMap(li -> li.getOrderable().getId(), identity(), (one, two) -> one));

      nonSkippedFullSupplyItems.forEach(requisitionLine -> {
        // ... we try to find line in POD for the same product ...
        ProofOfDeliveryLineItemDto proofOfDeliveryLine = productIdToPodLine.getOrDefault(
            requisitionLine.getOrderableId(), null);

        // ... and if line exists we set value for Total Received Quantity (B) column
        if (null != proofOfDeliveryLine) {
          requisitionLine.setTotalReceivedQuantity(
              OpenLmisNumberUtils.zeroIfNull(proofOfDeliveryLine.getQuantityAccepted())
          );
        }
      });
    }

    profiler.start("SET_PREV_ADJ_CONSUMPTION");
    setPreviousAdjustedConsumptions(numberOfPreviousPeriodsToAverage);

    status = RequisitionStatus.INITIATED;

    profiler.start("SET_STATUS_CHANGES");
    statusChanges.add(StatusChange.newStatusChange(this, initiator));

    profiler.stop().log();
  }

  private void initiateLineItems(Collection<ApprovedProductDto> fullSupplyProducts,
                                 Map<UUID, Integer> idealStockAmounts,
                                 Map<UUID, Integer> orderableSoh) {
    this.requisitionLineItems = new ArrayList<>();

    if (isNotTrue(emergency)) {
      for (ApprovedProductDto product : fullSupplyProducts) {
        Integer isa = extractIdealStockAmount(idealStockAmounts, product);
        Integer soh = orderableSoh.get(product.getOrderable().getId());

        RequisitionLineItem lineItem = new RequisitionLineItem(this, product, isa, soh);

        this.requisitionLineItems.add(lineItem);
      }
    }
  }

  private Integer extractIdealStockAmount(Map<UUID, Integer> idealStockAmounts,
                                          ApprovedProductDto product) {
    String commodityType = product.getOrderable().getCommodityTypeIdentifier();
    return isNotBlank(commodityType)
        ? idealStockAmounts.get(UUID.fromString(commodityType))
        : null;
  }

  /**
   * Submits this requisition.
   *
   * @param products orderable products that will be used by line items to update packs to ship.
   */
  public void submit(Collection<OrderableDto> products, UUID submitter, boolean skipAuthorize) {
    if (!status.isSubmittable()) {
      throw new ValidationMessageException(
          new Message(ERROR_MUST_BE_INITIATED_TO_BE_SUBMMITED, getId()));
    }

    if (RequisitionHelper.areFieldsNotFilled(template,
        getNonSkippedFullSupplyRequisitionLineItems())) {
      throw new ValidationMessageException(new Message(ERROR_FIELD_MUST_HAVE_VALUES, getId()));
    }

    updateConsumptions();
    updateTotalCostAndPacksToShip(products);

    status = RequisitionStatus.SUBMITTED;
    statusChanges.add(StatusChange.newStatusChange(this, submitter));

    if (skipAuthorize) {
      LOGGER.debug("Skipping authorize step.");
      populateApprovedQuantity();
      status = RequisitionStatus.AUTHORIZED;

      RequisitionHelper.forEachLine(getSkippedRequisitionLineItems(),
          RequisitionLineItem::resetData);
    }
  }

  /**
   * Authorize this Requisition.
   *
   * @param products orderable products that will be used by line items to update packs to ship.
   */
  public void authorize(Collection<OrderableDto> products, UUID authorizer) {
    if (!RequisitionStatus.SUBMITTED.equals(status)) {
      throw new ValidationMessageException(
          new Message(ERROR_MUST_BE_SUBMITTED_TO_BE_AUTHORIZED, getId()));
    }

    updateConsumptions();
    updateTotalCostAndPacksToShip(products);
    populateApprovedQuantity();

    status = RequisitionStatus.AUTHORIZED;
    RequisitionHelper.forEachLine(getSkippedRequisitionLineItems(), RequisitionLineItem::resetData);

    statusChanges.add(StatusChange.newStatusChange(this, authorizer));
  }

  /**
   * Check if the requisition is approvable.
   *
   */
  public boolean isApprovable() {
    return status.duringApproval();
  }

  /**
   * Checks whether the requisition status allows for its deletion.
   *
   * @return true if the requisition status is within those that allow deletion; false otherwise
   */
  public boolean isDeletable() {
    return isPreAuthorize() || status.isSkipped();
  }

  /**
   * Approves given requisition.
   *
   * @param nodeId      supervisoryNode that has a supply line for the requisition's program.
   * @param products    orderable products that will be used by line items to update packs to ship.
   * @param supplyLines supplyLineDtos of the supervisoryNode that has
   *                    a supply line for the requisition's program.
   * @param approver    user who approves this requisition.
   */
  public void approve(UUID nodeId, Collection<OrderableDto> products,
                      Collection<SupplyLineDto> supplyLines, UUID approver) {
    if (CollectionUtils.isEmpty(supplyLines) && nodeId != null) {
      status = RequisitionStatus.IN_APPROVAL;
      supervisoryNodeId = nodeId;
    } else {
      status = RequisitionStatus.APPROVED;
    }

    updateConsumptions();
    updateTotalCostAndPacksToShip(products);

    statusChanges.add(StatusChange.newStatusChange(this, approver));
  }

  /**
   * Rejects given requisition.
   */
  public void reject(Collection<OrderableDto> products, UUID rejector) {
    status = RequisitionStatus.REJECTED;
    updateConsumptions();
    updateTotalCostAndPacksToShip(products);

    statusChanges.add(StatusChange.newStatusChange(this, rejector));
  }

  /**
   * Release the requisition.
   */
  public void release(UUID releaser) {
    status = RequisitionStatus.RELEASED;
    statusChanges.add(StatusChange.newStatusChange(this, releaser));
  }

  /**
   * Finds first RequisitionLineItem that have productId property equals to the given productId
   * argument.
   *
   * @param productId UUID of orderable product
   * @return first RequisitionLineItem that have productId property equals to the given productId
   *         argument; otherwise null;
   */
  public RequisitionLineItem findLineByProductId(UUID productId) {
    if (null == requisitionLineItems) {
      return null;
    }

    return requisitionLineItems
        .stream()
        .filter(e -> Objects.equals(productId, e.getOrderableId()))
        .findFirst()
        .orElse(null);
  }

  public void setDraftStatusMessage(String draftStatusMessage) {
    this.draftStatusMessage = (draftStatusMessage == null) ? "" : draftStatusMessage;
  }

  public boolean isPreAuthorize() {
    return status.isPreAuthorize();
  }

  /**
   * Filter out requisitionLineItems that are skipped.
   *
   * @return requisitionLineItems that are not skipped
   */
  public List<RequisitionLineItem> getNonSkippedRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.isLineSkipped())
        .collect(toList());
  }

  /**
   * Filter out requisitionLineItems that are skipped and not-full supply.
   *
   * @return non-skipped full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedFullSupplyRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.isLineSkipped())
        .filter(line -> !line.isNonFullSupply())
        .collect(toList());
  }

  /**
   * Filter out requisitionLineItems that are skipped and full supply.
   *
   * @return non-skipped non-full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedNonFullSupplyRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.isLineSkipped())
        .filter(RequisitionLineItem::isNonFullSupply)
        .collect(toList());
  }

  /**
   * Filter out requisitionLineItems that are not skipped.
   *
   * @return requisitionLineItems that are skipped
   */
  public List<RequisitionLineItem> getSkippedRequisitionLineItems() {
    return this.requisitionLineItems.stream()
        .filter(RequisitionLineItem::isLineSkipped)
        .collect(toList());
  }

  /**
   * Calculates combined cost of all requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getTotalCost() {
    return calculateTotalCostForLines(requisitionLineItems);
  }

  /**
   * Calculates combined cost of non-full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getNonFullSupplyTotalCost() {
    return calculateTotalCostForLines(getNonSkippedNonFullSupplyRequisitionLineItems());
  }

  /**
   * Calculates combined cost of full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getFullSupplyTotalCost() {
    return calculateTotalCostForLines(getNonSkippedFullSupplyRequisitionLineItems());
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Requisition.Exporter exporter) {
    exporter.setId(id);
    exporter.setCreatedDate(getCreatedDate());
    exporter.setModifiedDate(getModifiedDate());
    exporter.setStatus(status);
    if (exporter.provideStatusChangeExporter().isPresent()) {
      for (StatusChange statusChange : statusChanges) {
        StatusChange.Exporter providedExporter = exporter.provideStatusChangeExporter().get();
        statusChange.export(providedExporter);
        exporter.addStatusChange(providedExporter);
      }
    }
    exporter.setEmergency(emergency);
    exporter.setSupplyingFacility(supplyingFacilityId);
    exporter.setSupervisoryNode(supervisoryNodeId);
    exporter.setDraftStatusMessage(draftStatusMessage);
    if (datePhysicalStockCountCompleted != null) {
      exporter.setDatePhysicalStockCountCompleted(
          datePhysicalStockCountCompleted.getLocalDate());
    }
  }

  /**
   * Find latest status change by date created.
   *
   * @return recent status change
   */
  public StatusChange getLatestStatusChange() {
    return statusChanges.stream()
        .max(Comparator.comparing(BaseTimestampedEntity::getCreatedDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }

  /**
   * Sets appropriate value for Previous Adjusted Consumptions field in
   * each {@link RequisitionLineItem}.
   */
  void setPreviousAdjustedConsumptions(int numberOfPreviousPeriodsToAverage) {
    List<RequisitionLineItem> previousRequisitionLineItems = RequisitionHelper
        .getNonSkippedLineItems(previousRequisitions.subList(0, numberOfPreviousPeriodsToAverage));

    RequisitionHelper.forEachLine(requisitionLineItems,
        line -> {
          List<RequisitionLineItem> withProductId = RequisitionHelper
              .findByProductId(previousRequisitionLineItems, line.getOrderableId());
          List<Integer> adjustedConsumptions = RequisitionHelper
              .mapToAdjustedConsumptions(withProductId);

          line.setPreviousAdjustedConsumptions(adjustedConsumptions);
        });
  }

  private Money calculateTotalCostForLines(List<RequisitionLineItem> requisitionLineItems) {
    Money defaultValue = Money.of(CurrencyUnit.of(CurrencyConfig.CURRENCY_CODE), 0);

    if (requisitionLineItems.isEmpty()) {
      return defaultValue;
    }

    Optional<Money> money = requisitionLineItems.stream()
        .map(RequisitionLineItem::getTotalCost).filter(Objects::nonNull).reduce(Money::plus);

    return money.orElse(defaultValue);
  }

  private void calculateAndValidateTemplateFields(RequisitionTemplate template) {
    getNonSkippedFullSupplyRequisitionLineItems()
        .forEach(line -> line.calculateAndSetFields(template, stockAdjustmentReasons,
            numberOfMonthsInPeriod));
  }

  private void updateConsumptions() {


    if (template.isColumnInTemplateAndDisplayed(ADJUSTED_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems().forEach(line -> line.setAdjustedConsumption(
          LineItemFieldsCalculator.calculateAdjustedConsumption(line, numberOfMonthsInPeriod)
      ));
    }

    if (template.isColumnInTemplateAndDisplayed(AVERAGE_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems().forEach(
          RequisitionLineItem::calculateAndSetAverageConsumption);
    }
  }

  private void updateTotalCostAndPacksToShip(Collection<OrderableDto> products) {
    getNonSkippedRequisitionLineItems().forEach(line -> line.updatePacksToShip(products));

    getNonSkippedRequisitionLineItems().forEach(line -> line.setTotalCost(
        LineItemFieldsCalculator.calculateTotalCost(line,
            CurrencyUnit.of(CurrencyConfig.CURRENCY_CODE))
    ));
  }

  private void populateApprovedQuantity() {
    if (template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY)) {
      getNonSkippedRequisitionLineItems().forEach(line -> {
        if (isNull(line.getRequestedQuantity())) {
          line.setApprovedQuantity(line.getCalculatedOrderQuantity());
        } else {
          line.setApprovedQuantity(line.getRequestedQuantity());
        }
      });
    } else {
      getNonSkippedRequisitionLineItems().forEach(line ->
          line.setApprovedQuantity(line.getRequestedQuantity())
      );
    }
  }

  private void updateReqLines(Collection<RequisitionLineItem> newLineItems) {
    if (null == newLineItems) {
      return;
    }

    if (null == requisitionLineItems) {
      requisitionLineItems = new ArrayList<>();
    }

    List<RequisitionLineItem> updatedList = new ArrayList<>();

    for (RequisitionLineItem item : newLineItems) {
      RequisitionLineItem existing = requisitionLineItems
          .stream()
          .filter(l -> l.getId().equals(item.getId()))
          .findFirst()
          .orElse(null);

      if (null == existing) {
        if (isTrue(emergency) || item.isNonFullSupply()) {
          item.setRequisition(this);
          updatedList.add(item);
        }
      } else {
        existing.setRequisition(this);
        existing.updateFrom(item);
        updatedList.add(existing);
      }
    }

    if (isNotTrue(emergency)) {
      // is there a full supply line that is not in update list
      // it should be added. Those lines should not be removed
      // during update. Only non full supply lines can be
      // added/updated/removed.
      List<UUID> updatedIds = updatedList.stream().map(BaseEntity::getId).collect(toList());

      requisitionLineItems
          .stream()
          .filter(line -> !line.isNonFullSupply())
          .filter(line -> !updatedIds.contains(line.getId()))
          .forEach(updatedList::add);
    }

    requisitionLineItems.clear();
    requisitionLineItems.addAll(updatedList);
  }

  public interface Exporter {
    void setId(UUID id);

    void setCreatedDate(ZonedDateTime createdDate);

    void setModifiedDate(ZonedDateTime createdDate);

    void setStatus(RequisitionStatus status);

    void setEmergency(Boolean emergency);

    void setSupplyingFacility(UUID supplyingFacility);

    void setSupervisoryNode(UUID supervisoryNode);

    void setTemplate(BasicRequisitionTemplateDto template);

    void setDraftStatusMessage(String draftStatusMessage);

    void setDatePhysicalStockCountCompleted(LocalDate localDate);

    void setStockAdjustmentReasons(List<ReasonDto> reasonDto);

    Optional<StatusChange.Exporter> provideStatusChangeExporter();

    void addStatusChange(StatusChange.Exporter providedExporter);
  }

  public interface Importer {
    UUID getId();

    ZonedDateTime getCreatedDate();

    ZonedDateTime getModifiedDate();

    List<RequisitionLineItem.Importer> getRequisitionLineItems();

    FacilityDto getFacility();

    ProgramDto getProgram();

    ProcessingPeriodDto getProcessingPeriod();

    RequisitionStatus getStatus();

    Boolean getEmergency();

    UUID getSupplyingFacility();

    UUID getSupervisoryNode();

    String getDraftStatusMessage();

    Set<OrderableDto> getAvailableNonFullSupplyProducts();

    LocalDate getDatePhysicalStockCountCompleted();
  }
}