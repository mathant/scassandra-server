/*
 * Copyright (C) 2017 Christopher Batey and Dogan Narinc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scassandra.codec.datatype

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scassandra.codec.ProtocolVersion
import scodec.Attempt.Failure
import scodec.bits.ByteVector

class MapSpec extends DataTypeSpec with TableDrivenPropertyChecks {
  val protocolVersions = Table("Protocol Version", ProtocolVersion.versions: _*)

  forAll(protocolVersions) { (protocolVersion: ProtocolVersion) =>
    implicit val protocol: ProtocolVersion = protocolVersion
    val codec = CqlMap(Varchar, Varchar).codec
    val nestedCodec = CqlMap(Varchar, CqlSet(Varchar)).codec

    val expectedBytes = if (protocol.version < 3) {
      ByteVector(
        0, 2, // number of elements
        0, 3, 111, 110, 101, // one
        0, 3, 116, 119, 111, // two
        0, 5, 116, 104, 114, 101, 101, // three
        0, 4, 102, 111, 117, 114 // four
      )
    } else {
      ByteVector(
        0, 0, 0, 2, // number of elements
        0, 0, 0, 3, 111, 110, 101, // one
        0, 0, 0, 3, 116, 119, 111, // two
        0, 0, 0, 5, 116, 104, 114, 101, 101, // three
        0, 0, 0, 4, 102, 111, 117, 114 // four
      )
    }

    "codec with " + protocol must "encode map<varchar, varchar> from a Map" in {
      codec.encode(Map("one" -> "two", "three" -> "four")).require.bytes shouldEqual expectedBytes
    }

    it must "encode empty Map properly" in {
      val zeros = if (protocol.version < 3) {
        ByteVector(0, 0)
      } else {
        ByteVector(0, 0, 0, 0)
      }

      codec.encode(Map()).require.bytes shouldEqual zeros
    }

    val expectedNestedBytes = if (protocol.version < 3) {
      ByteVector(
        0, 2, // number of elements
        0, 5, 102, 105, 114, 115, 116, // first
        0, 19, // byte length of first set
        0, 3, // elements in set
        0, 3, 111, 110, 101, // one
        0, 3, 116, 119, 111, // two
        0, 5, 116, 104, 114, 101, 101, // three
        0, 6, 115, 101, 99, 111, 110, 100, // second
        0, 19, // byte length of second set
        0, 3, // elements in set
        0, 4, 102, 111, 117, 114, // four
        0, 4, 102, 105, 118, 101, // five
        0, 3, 115, 105, 120 // six
      )
    } else {
      ByteVector(
        0, 0, 0, 2, // number of elements
        0, 0, 0, 5, 102, 105, 114, 115, 116, // first
        0, 0, 0, 27, // byte length of first set
        0, 0, 0, 3, // elements in set
        0, 0, 0, 3, 111, 110, 101, // one
        0, 0, 0, 3, 116, 119, 111, // two
        0, 0, 0, 5, 116, 104, 114, 101, 101, // three
        0, 0, 0, 6, 115, 101, 99, 111, 110, 100, // second
        0, 0, 0, 27, // byte length of second set
        0, 0, 0, 3, // elements in set
        0, 0, 0, 4, 102, 111, 117, 114, // four
        0, 0, 0, 4, 102, 105, 118, 101, // five
        0, 0, 0, 3, 115, 105, 120 // six
      )
    }

    it must "encode map<varchar, set<varchar>> from a Map" in {
      nestedCodec.encode(Map("first" -> Set("one", "two", "three"), "second" -> Set("four", "five", "six"))).require.bytes shouldEqual expectedNestedBytes
    }

    it must "fail to encode type that is not a Map" in {
      codec.encode("0x01") should matchPattern { case Failure(_) => }
      codec.encode("1235") should matchPattern { case Failure(_) => }
      codec.encode(BigDecimal("123.67")) should matchPattern { case Failure(_) => }
      codec.encode(true) should matchPattern { case Failure(_) => }
      codec.encode(false) should matchPattern { case Failure(_) => }
      codec.encode(List()) should matchPattern { case Failure(_) => }
      codec.encode(Set()) should matchPattern { case Failure(_) => }
    }

    it must "encode and decode back as Map" in {
      encodeAndDecode(codec, Map())
      encodeAndDecode(codec, Map("one" -> "two", "three" -> "four"))

      val nestedMap = Map("first" -> Set("one", "two", "three"), "second" -> Set("four", "five", "six"))
      encodeAndDecode(nestedCodec, nestedMap)
      encodeAndDecode(nestedCodec, Map("first" -> List("one", "two", "three"), "second" -> List("four", "five", "six")), nestedMap)
    }
  }

}
