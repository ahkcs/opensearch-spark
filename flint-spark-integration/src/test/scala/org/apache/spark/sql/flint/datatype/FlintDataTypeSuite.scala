/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.sql.flint.datatype

import org.json4s.JValue
import org.json4s.jackson.JsonMethods
import org.opensearch.flint.spark.udt.GeoPointUDT
import org.scalatest.matchers.should.Matchers

import org.apache.spark.FlintSuite
import org.apache.spark.sql.flint.datatype.FlintMetadataExtensions.MetadataBuilderExtension
import org.apache.spark.sql.types._

class FlintDataTypeSuite extends FlintSuite with Matchers {
  test("basic deserialize and serialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "booleanField": {
                          |      "type": "boolean"
                          |    },
                          |    "keywordField": {
                          |      "type": "keyword"
                          |    },
                          |    "longField": {
                          |      "type": "long"
                          |    },
                          |    "integerField": {
                          |      "type": "integer"
                          |    },
                          |    "shortField": {
                          |      "type": "short"
                          |    },
                          |    "byteField": {
                          |      "type": "byte"
                          |    },
                          |    "doubleField": {
                          |      "type": "double"
                          |    },
                          |    "floatField": {
                          |      "type": "float"
                          |    },
                          |    "halfFloatField": {
                          |      "type": "half_float"
                          |    },
                          |    "textField": {
                          |      "type": "text"
                          |    },
                          |    "binaryField": {
                          |      "type": "binary",
                          |      "doc_values": true
                          |    }
                          |  }
                          |}""".stripMargin
    val sparkStructType = StructType(
      StructField("booleanField", BooleanType, true) ::
        StructField("keywordField", StringType, true) ::
        StructField("longField", LongType, true) ::
        StructField("integerField", IntegerType, true) ::
        StructField("shortField", ShortType, true) ::
        StructField("byteField", ByteType, true) ::
        StructField("doubleField", DoubleType, true) ::
        StructField("floatField", FloatType, true) ::
        StructField(
          "halfFloatField",
          FloatType,
          true,
          new MetadataBuilder().putString("osType", "half_float").build()) ::
        StructField(
          "textField",
          StringType,
          true,
          new MetadataBuilder().putString("osType", "text").build()) ::
        StructField("binaryField", BinaryType, true) ::
        Nil)

    FlintDataType.serialize(sparkStructType) shouldBe compactJson(flintDataType)
    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs sparkStructType
  }

  test("deserialize unsupported flint data type throw exception") {
    val unsupportedField = """{
      "properties": {
        "rangeField": {
          "type": "integer_range"
        }
      }
    }"""
    an[IllegalStateException] should be thrownBy FlintDataType.deserialize(unsupportedField)
  }

  test("flint object type deserialize and serialize") {
    val flintDataType = """{
                             |  "properties": {
                             |    "object1": {
                             |      "properties": {
                             |        "shortField": {
                             |          "type": "short"
                             |        }
                             |      }
                             |    },
                             |    "object2": {
                             |      "type": "object",
                             |      "properties": {
                             |        "integerField": {
                             |          "type": "integer"
                             |        }
                             |      }
                             |    }
                             |  }
                             |}""".stripMargin
    val sparkStructType = StructType(
      StructField("object1", StructType(StructField("shortField", ShortType) :: Nil)) ::
        StructField("object2", StructType(StructField("integerField", IntegerType) :: Nil)) ::
        Nil)

    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs sparkStructType

    FlintDataType.serialize(sparkStructType) shouldBe compactJson("""{
                                                                    |  "properties": {
                                                                    |    "object1": {
                                                                    |      "properties": {
                                                                    |        "shortField": {
                                                                    |          "type": "short"
                                                                    |        }
                                                                    |      }
                                                                    |    },
                                                                    |    "object2": {
                                                                    |      "properties": {
                                                                    |        "integerField": {
                                                                    |          "type": "integer"
                                                                    |        }
                                                                    |      }
                                                                    |    }
                                                                    |  }
                                                                    |}""".stripMargin)
  }

  test("spark map type serialize") {
    val sparkStructType = StructType(
      StructField("mapField", MapType(StringType, StringType), true) ::
        Nil)

    FlintDataType.serialize(sparkStructType) shouldBe compactJson("""{
                                                                    |  "properties": {
                                                                    |    "mapField": {
                                                                    |      "properties": {
                                                                    |      }
                                                                    |    }
                                                                    |  }
                                                                    |}""".stripMargin)
  }

  test("spark decimal type serialize") {
    val sparkStructType = StructType(
      StructField("decimalField", DecimalType(1, 1), true) ::
        Nil)

    FlintDataType.serialize(sparkStructType) shouldBe compactJson("""{
                                                                    |  "properties": {
                                                                    |    "decimalField": {
                                                                    |      "type": "double"
                                                                    |    }
                                                                    |  }
                                                                    |}""".stripMargin)
  }

  test("spark varchar and char type serialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "varcharField": {
                          |      "type": "keyword"
                          |    },
                          |    "charField": {
                          |      "type": "keyword"
                          |    }
                          |  }
                          |}""".stripMargin
    val sparkStructType = StructType(
      StructField("varcharField", VarcharType(20), true) ::
        StructField("charField", CharType(20), true) ::
        Nil)
    FlintDataType.serialize(sparkStructType) shouldBe compactJson(flintDataType)
    // flint data type should not deserialize to varchar or char
    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs StructType(
      StructField("varcharField", StringType, true) ::
        StructField("charField", StringType, true) ::
        Nil)
  }

  test("spark varchar and char type with osType text serialize") {
    val flintDataType =
      """{
        |  "properties": {
        |    "varcharTextField": {
        |      "type": "text"
        |    },
        |    "charTextField": {
        |      "type": "text"
        |    }
        |  }
        |}""".stripMargin
    val textMetadata = new MetadataBuilder().putString("osType", "text").build()
    val sparkStructType = StructType(
      StructField("varcharTextField", VarcharType(20), true, textMetadata) ::
        StructField("charTextField", CharType(20), true, textMetadata) ::
        Nil)
    FlintDataType.serialize(sparkStructType) shouldBe compactJson(flintDataType)
    // flint data type should not deserialize to varchar or char
    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs StructType(
      StructField("varcharTextField", StringType, true, textMetadata) ::
        StructField("charTextField", StringType, true, textMetadata) ::
        Nil)
  }

  test("text field with multi-fields deserialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "city": {
                          |      "type": "text",
                          |      "fields": {
                          |        "raw": {
                          |          "type": "keyword"
                          |        },
                          |        "keyword": {
                          |          "type": "keyword"
                          |        }
                          |      }
                          |    }
                          |  }
                          |}""".stripMargin
    val metadata = new MetadataBuilder().withTextField
      .withMultiFields(Map("city.raw" -> "keyword", "city.keyword" -> "keyword"))
      .build()
    val expectedStructType = StructType(StructField("city", StringType, true, metadata) :: Nil)

    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs expectedStructType
  }

  test("text field without multi-fields deserialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "description": {
                          |      "type": "text"
                          |    }
                          |  }
                          |}""".stripMargin

    val metadata = new MetadataBuilder().withTextField().build()

    val expectedStructType =
      StructType(StructField("description", StringType, true, metadata) :: Nil)
    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs expectedStructType
  }

  test("flint date type deserialize and serialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "timestampField": {
                          |      "type": "date",
                          |      "format": "strict_date_optional_time_nanos"
                          |    },
                          |    "dateField": {
                          |      "type": "date",
                          |      "format": "strict_date"
                          |    }
                          |  }
                          |}""".stripMargin
    val sparkStructType = StructType(
      StructField("timestampField", TimestampType, true) ::
        StructField("dateField", DateType, true) ::
        Nil)
    FlintDataType.deserialize(flintDataType) should contain theSameElementsAs sparkStructType
    FlintDataType.serialize(sparkStructType) shouldBe compactJson(flintDataType)
  }

  test("flint timestamp_ntz serialize") {
    val flintDataType = """{
                          |  "properties": {
                          |    "timestampField": {
                          |      "type": "date",
                          |      "format": "strict_date_optional_time_nanos"
                          |    }
                          |  }
                          |}""".stripMargin
    val sparkStructType = StructType(
      StructField("timestampField", TimestampNTZType, true) ::
        Nil)
    FlintDataType.serialize(sparkStructType) shouldBe compactJson(flintDataType)
  }

  test("spark array type map to should map to array element type in OpenSearch") {
    val flintDataType = """{
                          |  "properties": {
                          |    "varcharField": {
                          |      "type": "keyword"
                          |    },
                          |    "charField": {
                          |      "type": "keyword"
                          |    }
                          |  }
                          |}""".stripMargin
    val sparkStructType =
      StructType(
        StructField("arrayIntField", ArrayType(IntegerType), true) ::
          StructField(
            "arrayObjectField",
            StructType(StructField("booleanField", BooleanType, true) :: Nil),
            true) :: Nil)
    FlintDataType.serialize(sparkStructType) shouldBe compactJson(s"""{
         |  "properties": {
         |    "arrayIntField": {
         |      "type": "integer"
         |    },
         |    "arrayObjectField": {
         |      "properties": {
         |        "booleanField":{
         |          "type": "boolean"
         |        }
         |      }
         |    }
         |  }
         |}""".stripMargin)
  }

  def compactJson(json: String): String = {
    val data: JValue = JsonMethods.parse(json)
    JsonMethods.compact(JsonMethods.render(data))
  }

  test("alias field deserialize") {
    val flintDataType =
      """{
        |  "properties": {
        |    "distance": {
        |      "type": "long"
        |    },
        |    "route_length_miles": {
        |      "type": "alias",
        |      "path": "distance"
        |    },
        |    "transit_mode": {
        |      "type": "keyword"
        |    }
        |  }
        |}""".stripMargin

    val expectedStructType = StructType(
      Seq(
        StructField("distance", LongType, true),
        StructField("transit_mode", StringType, true),
        StructField(
          "route_length_miles",
          LongType,
          true,
          new MetadataBuilder().putString("aliasPath", "distance").build())))

    val deserialized = FlintDataType.deserialize(flintDataType)

    deserialized.fields should have length (3)
    deserialized.fields(0) shouldEqual expectedStructType.fields(0)
    deserialized.fields(1) shouldEqual expectedStructType.fields(1)

    val aliasField = deserialized.fields(2)
    aliasField.name shouldEqual "route_length_miles"
    aliasField.dataType shouldEqual LongType
    aliasField.metadata.contains("aliasPath") shouldBe true
    aliasField.metadata.getString("aliasPath") shouldEqual "distance"
  }

  test("geo_point field deserialize") {
    val flintDataType =
      """{
        |  "properties": {
        |    "location": {
        |      "type": "geo_point"
        |    }
        |  }
        |}""".stripMargin

    val expectedStructType = StructType(StructField("location", GeoPointUDT, true) :: Nil)

    val deserialized = FlintDataType.deserialize(flintDataType)

    deserialized.fields should have length (1)
    deserialized.fields(0) shouldEqual expectedStructType.fields(0)
  }
}
