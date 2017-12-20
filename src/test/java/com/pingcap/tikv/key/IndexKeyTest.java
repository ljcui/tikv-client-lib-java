/*
 * Copyright 2017 PingCAP, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pingcap.tikv.key;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pingcap.tikv.types.IntegerType;
import org.junit.Test;


public class IndexKeyTest {

  @Test
  public void createTest() throws Exception {
    Key k1 = Key.toKey(new byte[] {1,2,3,4});
    Key k2 = Key.toKey(new byte[] {5,6,7,8});
    Key k3 = Key.toKey(new byte[] {5,6,7,9});
    IndexKey ik1 = IndexKey.create(666, 777, k1, k2);
    IndexKey ik2 = IndexKey.create(666, 777, k1, k2);
    IndexKey ik3 = IndexKey.create(666, 777, k1, k3);
    assertEquals(ik1, ik2);
    assertTrue(ik1.compareTo(ik2) == 0);
    assertTrue(ik1.compareTo(ik3) < 0);
    assertEquals(2, ik1.getDataKeys().length);
    assertEquals(k1, ik1.getDataKeys()[0]);
    assertEquals(k2, ik1.getDataKeys()[1]);

    IndexKey ik4 = IndexKey.create(0, 0, k1, null, k2);
    IndexKey ik5 = IndexKey.create(0, 0, k1, Key.NULL, k2);
    assertEquals(ik4, ik5);
  }

  @Test
  public void toStringTest() throws Exception {
    Key k1 = Key.toKey(new byte[] {1,2,3,4});
    TypedKey k2 = TypedKey.create(666, IntegerType.DEF_LONG_TYPE);
    IndexKey ik = IndexKey.create(0, 0, k1, Key.NULL, k2);
    assertEquals("[{1,2,3,4},null,666]", ik.toString());
  }

}