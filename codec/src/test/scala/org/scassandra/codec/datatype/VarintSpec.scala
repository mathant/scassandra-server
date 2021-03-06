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

import scodec.Attempt.Failure
import scodec.bits.ByteVector

class VarintSpec extends DataTypeSpec {

  val codec = Varint.codec

  "codec" must "encode BigInt as varint format" in {
    codec.encode(BigInt("123000000000")).require.bytes shouldEqual ByteVector(28, -93, 95, 14, 0)
  }

  it must "encode decimal Number as varint format" in {
    codec.encode(BigDecimal("123000000000")).require.bytes shouldEqual ByteVector(28, -93, 95, 14, 0)
    codec.encode(0).require.bytes shouldEqual ByteVector(0)
  }

  it must "fail to encode value that doesn't map to a varint" in {
    codec.encode("hello") should matchPattern { case Failure(_) => }
    codec.encode(true) should matchPattern { case Failure(_) => }
    codec.encode(false) should matchPattern { case Failure(_) => }
    codec.encode(List()) should matchPattern { case Failure(_) => }
    codec.encode(Map()) should matchPattern { case Failure(_) => }
  }

  it must "encode and decode back to input as BigInt" in {
    encodeAndDecode(codec, BigInt("123000000000"))
    encodeAndDecode(codec, 123, BigInt(123))
    encodeAndDecode(codec, "1234", BigInt(1234))
  }

}
