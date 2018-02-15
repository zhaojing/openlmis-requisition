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

package org.openlmis.requisition.testutils;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrderableDtoDataBuilder {
  private static int instanceNumber = 0;

  private UUID id;
  private String productCode;
  private String fullProductName;
  private long netContent;
  private long packRoundingThreshold;
  private boolean roundToZero;
  private Set<ProgramOrderableDto> programs;
  private DispensableDto dispensable;
  private Map<String, String> identifiers;

  /**
   * Creates builder for creating new instance of {@link OrderableDtoDataBuilder}.
   */
  public OrderableDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    productCode = "P" + instanceNumber;
    fullProductName = "Product " + instanceNumber;
    netContent = 1;
    packRoundingThreshold = 1;
    roundToZero = true;
    programs = new HashSet<>();
    dispensable = new DispensableDto("pack");
    identifiers = new HashMap<>();
  }

  /**
   * Add program orderable with passed properties.
   */
  public OrderableDtoDataBuilder withProgramOrderable(UUID programId, boolean fullSupply) {
    this.programs.add(new ProgramOrderableDto(
        programId, null, null, null, fullSupply, null, Money.of(CurrencyUnit.USD, 1)
    ));

    return this;
  }

  public OrderableDtoDataBuilder withIdentifier(String key, String value) {
    identifiers.put(key, value);
    return this;
  }

  public OrderableDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Creates new instance of {@link OrderableDto} with properties.
   * @return created orderable.
   */
  public OrderableDto build() {
    OrderableDto dto = new OrderableDto();
    dto.setId(id);
    dto.setProductCode(productCode);
    dto.setFullProductName(fullProductName);
    dto.setNetContent(netContent);
    dto.setPackRoundingThreshold(packRoundingThreshold);
    dto.setRoundToZero(roundToZero);
    dto.setPrograms(programs);
    dto.setDispensable(dispensable);
    dto.setIdentifiers(identifiers);

    return dto;
  }
}