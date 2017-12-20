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

package com.pingcap.tikv.codec;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.primitives.UnsignedLong;
import com.pingcap.tikv.codec.Codec.DateTimeCodec;
import com.pingcap.tikv.codec.Codec.DecimalCodec;
import com.pingcap.tikv.codec.Codec.IntegerCodec;
import com.pingcap.tikv.codec.Codec.RealCodec;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.junit.Test;

public class CodecTest {
  @Test
  public void writeDoubleAndReadDoubleTest() {
    // issue scientific notation in toBin
    CodecDataOutput cdo = new CodecDataOutput();
    DecimalCodec.writeDouble(cdo, 0.01);
    double u = DecimalCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(0.01, u, 0.0001);

    cdo.reset();
    DecimalCodec.writeDouble(cdo, 206.0);
    u = DecimalCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(206.0, u, 0.0001);

    cdo.reset();
    DecimalCodec.writeDecimal(cdo, BigDecimal.valueOf(206.0));
    u = DecimalCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(206.0, u, 0.0001);
  }

  @Test
  public void readNWriteLongTest() throws Exception {
    CodecDataOutput cdo = new CodecDataOutput();
    IntegerCodec.writeLongFull(cdo, 9999L, true);
    IntegerCodec.writeLongFull(cdo, -2333L, false);
    assertArrayEquals(
        new byte[] {
            (byte) 0x3,
            (byte) 0x80,
            (byte) 0x0,
            (byte) 0x0,
            (byte) 0x0,
            (byte) 0x0,
            (byte) 0x0,
            (byte) 0x27,
            (byte) 0xf,
            (byte) 0x8,
            (byte) 0xb9,
            (byte) 0x24
        },
        cdo.toBytes());
    CodecDataInput cdi = new CodecDataInput(cdo.toBytes());
    long value = IntegerCodec.readLongFully(cdi);
    assertEquals(9999L, value);
    value = IntegerCodec.readLongFully(cdi);
    assertEquals(-2333L, value);

    byte[] wrongData = new byte[] {(byte) 0x8, (byte) 0xb9};
    cdi = new CodecDataInput(wrongData);
    try {
      IntegerCodec.readLongFully(cdi);
      fail();
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void readNWriteUnsignedLongTest() throws Exception {
    CodecDataOutput cdo = new CodecDataOutput();
    IntegerCodec.writeULongFull(cdo, 0xffffffffffffffffL, true);
    IntegerCodec.writeULongFull(cdo, Long.MIN_VALUE, false);
    assertArrayEquals(
        new byte[] {
            (byte) 0x4,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0x9,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x80,
            (byte) 0x1
        },
        cdo.toBytes());
    CodecDataInput cdi = new CodecDataInput(cdo.toBytes());
    long value = IntegerCodec.readULongFully(cdi);

    assertEquals(0xffffffffffffffffL, value);
    value = IntegerCodec.readULongFully(cdi);
    assertEquals(Long.MIN_VALUE, value);

    byte[] wrongData =
        new byte[] {
            (byte) 0x9, (byte) 0x80, (byte) 0x80, (byte) 0x80,
            (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
            (byte) 0x80, (byte) 0x80
        };
    cdi = new CodecDataInput(wrongData);
    try {
      IntegerCodec.readULongFully(cdi);
      fail();
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void writeFloatTest() throws Exception {
    CodecDataOutput cdo = new CodecDataOutput();
    RealCodec.writeDouble(cdo, 0.00);
    double u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(0.00, u, 0);

    cdo.reset();
    RealCodec.writeDouble(cdo, Double.MAX_VALUE);
    u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(Double.MAX_VALUE, u, 0);

    cdo.reset();
    RealCodec.writeDouble(cdo, Double.MIN_VALUE);
    u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(Double.MIN_VALUE, u, 0);
  }

  @Test
  public void readFloatTest() throws Exception {
    byte[] data =
        new byte[] {
            (byte) (191 & 0xFF),
            (byte) (241 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (160 & 0xFF),
            0,
            0,
            0
        };
    CodecDataInput cdi = new CodecDataInput(data);
    double u = RealCodec.readDouble(cdi);
    assertEquals(1.1, u, 0.0001);

    data =
        new byte[] {
            (byte) (192 & 0xFF),
            (byte) (1 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (153 & 0xFF),
            (byte) (154 & 0xFF)
        };
    cdi = new CodecDataInput(data);
    u = RealCodec.readDouble(cdi);
    assertEquals(2.2, u, 0.0001);

    data =
        new byte[] {
            (byte) (63 & 0xFF),
            (byte) (167 & 0xFF),
            (byte) (51 & 0xFF),
            (byte) (67 & 0xFF),
            (byte) (159 & 0xFF),
            (byte) (0xFF),
            (byte) (0xFF),
            (byte) (0xFF)
        };

    cdi = new CodecDataInput(data);
    u = RealCodec.readDouble(cdi);
    assertEquals(-99.199, u, 0.0001);
  }

  @Test
  public void negativeLongTest() throws Exception {
    CodecDataOutput cdo = new CodecDataOutput();
    IntegerCodec.writeULong(cdo, UnsignedLong.valueOf("13831004815617530266").longValue());
    double u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(1.1, u, 0.001);

    cdo.reset();
    IntegerCodec.writeULong(cdo, UnsignedLong.valueOf("13835508415244900762").longValue());
    u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(2.2, u, 0.001);

    cdo.reset();
    IntegerCodec.writeULong(cdo, UnsignedLong.valueOf("13837985394932580352").longValue());
    u = RealCodec.readDouble(new CodecDataInput(cdo.toBytes()));
    assertEquals(3.3, u, 0.001);
  }

  @Test
  public void fromPackedLongAndToPackedLongTest() throws ParseException {

    LocalDateTime time = LocalDateTime.of(1999, 12, 12, 1, 1, 1, 1000);
    LocalDateTime time1 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time));
    assertEquals(time, time1);

    // since precision is microseconds, any nanoseconds is smaller than 1000 will be dropped.
    time = LocalDateTime.of(1999, 12, 12, 1, 1, 1, 1);
    time1 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time));
    assertNotEquals(time, time1);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSSSSSS");
    LocalDateTime time2 = LocalDateTime.parse("2010-10-10 10:11:11:0000000", formatter);
    LocalDateTime time3 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time2));
    assertEquals(time2, time3);

    // when packedLong is 0, then null is returned
    LocalDateTime time4 = DateTimeCodec.fromPackedLong(0);
    assertNull(time4);

    LocalDateTime time5 = LocalDateTime.parse("9999-12-31 23:59:59:0000000", formatter);
    LocalDateTime time6 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time5));
    assertEquals(time5, time6);

    LocalDateTime time7 = LocalDateTime.parse("1000-01-01 00:00:00:0000000", formatter);
    LocalDateTime time8 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time7));
    assertEquals(time7, time8);

    LocalDateTime time9 = LocalDateTime.parse("2017-01-05 23:59:59:5756010", formatter);
    LocalDateTime time10 = DateTimeCodec.fromPackedLong(DateTimeCodec.toPackedLong(time9));
    assertEquals(time9, time10);

    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
    Date date1 = formatter1.parse("2099-10-30");
    long time11 = DateTimeCodec.toPackedLong(date1);
    LocalDateTime time12 = DateTimeCodec.fromPackedLong(time11);
    assertEquals(time12.toLocalDate(), new java.sql.Date(date1.getTime()).toLocalDate());
  }
}