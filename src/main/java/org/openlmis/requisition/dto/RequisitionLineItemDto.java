package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.joda.money.Money;
import org.openlmis.requisition.domain.RequisitionLineItem;
import org.openlmis.requisition.domain.StockAdjustment;
import org.openlmis.utils.MoneyDeserializer;
import org.openlmis.utils.MoneySerializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequisitionLineItemDto
    implements RequisitionLineItem.Exporter, RequisitionLineItem.Importer {

  private UUID id;
  private OrderableProductDto orderableProduct;
  private Integer beginningBalance;
  private Integer totalReceivedQuantity;
  private Integer totalLossesAndAdjustments;
  private Integer stockOnHand;
  private Integer requestedQuantity;
  private Integer totalConsumedQuantity;
  private String requestedQuantityExplanation;
  private String remarks;
  private Integer approvedQuantity;
  private Integer totalStockoutDays;
  private Integer total;
  private Long packsToShip;
  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money pricePerPack;
  private Integer numberOfNewPatientsAdded;
  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money totalCost;
  private Boolean skipped;
  private Integer adjustedConsumption;
  private List<Integer> previousAdjustedConsumptions;
  private Integer averageConsumption;
  private BigDecimal maxMonthsOfStock;
  private Integer maximumStockQuantity;
  private Integer calculatedOrderQuantity;
  private String productCategoryDisplayName;

  @JsonProperty
  private List<StockAdjustmentDto> stockAdjustments;

  @JsonIgnore
  @Override
  public List<StockAdjustment.Importer> getStockAdjustments() {
    if (stockAdjustments == null) {
      return Collections.emptyList();
    }

    List<StockAdjustment.Importer> stockAdjustmentImporters = new ArrayList<>();
    stockAdjustmentImporters.addAll(stockAdjustments);
    return stockAdjustmentImporters;
  }

  @JsonIgnore
  @Override
  public void setStockAdjustments(List<StockAdjustment> stockAdjustments) {
    if (stockAdjustments != null) {
      List<StockAdjustmentDto> stockAdjustmentDtos = new ArrayList<>();

      for (StockAdjustment stockAdjustment : stockAdjustments) {
        StockAdjustmentDto stockAdjustmentDto = new StockAdjustmentDto();
        stockAdjustment.export(stockAdjustmentDto);
        stockAdjustmentDtos.add(stockAdjustmentDto);
      }

      this.stockAdjustments = stockAdjustmentDtos;
    } else {
      this.stockAdjustments = null;
    }
  }
}
