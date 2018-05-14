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

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.testutils.ToStringTestUtils;

public class SkipParamsTest {

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SkipParams.class)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    SkipParams creationDetails = new SkipParams(true, UUID.randomUUID());
    ToStringTestUtils.verify(SkipParams.class, creationDetails);
  }

}
