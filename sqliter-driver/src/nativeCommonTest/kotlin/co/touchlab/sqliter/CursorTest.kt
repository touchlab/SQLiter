/*
 * Copyright (C) 2018 Touchlab, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.sqliter

import kotlin.test.Test
import kotlin.test.assertEquals

class CursorTest:BaseDatabaseTest(){
    @Test
    fun iterator(){
        val manager = createDatabaseManager(DatabaseConfiguration(name = TEST_DB_NAME, version = 1,
            journalMode = JournalMode.WAL,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            }))

        val connection = manager.surpriseMeConnection()
        connection.withStatement("insert into test(num, str)values(?,?)"){
            bindLong(1, 2)
            bindString(2, "asdf")
            executeInsert()
            bindLong(1, 3)
            bindString(2, "qwert")
            executeInsert()
        }

        connection.withStatement("select * from test"){
            var rowCount = 0
            query().iterator().forEach {
                if(rowCount == 0){
                    assertEquals(it.values.get(0).second as Long, 2)
                    assertEquals(it.values.get(1).second as String, "asdf")
                }else if(rowCount == 0){
                    assertEquals(it.values.get(0).second as Long, 3)
                    assertEquals(it.values.get(1).second as String, "qwert")
                }

                rowCount++
            }

            assertEquals(2, rowCount)
        }

        connection.close()
    }

    @Test
    fun testUtf8() {
        val manager = createDatabaseManager(DatabaseConfiguration(name = TEST_DB_NAME, version = 1,
            journalMode = JournalMode.WAL,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            }))

        val connection = manager.surpriseMeConnection()
        connection.withStatement("insert into test(num, str)values(?,?)"){
            bindLong(1, 2)
            bindString(2, utf8stress)
            executeInsert()
        }

        connection.withStatement("select * from test"){
            query().iterator().next().let {
                val dbVal = it.values.get(1).second as String
                assertEquals(dbVal.length, utf8stress.length)
                assertEquals(dbVal, utf8stress)
            }
        }

        connection.close()
    }

    val utf8stress = """𝚊ḡηӑ 𝑓ṟĭṅᶃℹɬŀā ựᶉπ𝓪 ṗ૦𝓻τṯí𝞃ỡ𝐫 𝓻𝘩ỡ𝙣ϲų𝐬 𝖽𝖔ɭοꝛ ṕü𝘳ũ𝑠. Ł𝝸ճȩⲅò 𝗇ųղ𝑐 сṏդṣẹ𝓺𝓊α𝙩 ᶖηȶ𝖾ṝɖųᵯ 𝞶𝛂яᶖǖ𝗌 𝑠і𝜏 𝝰ṁėτ. Ở𝖽ĭ𝞸 ḟ𝗮ⲥ𝙞ł𝙞ꞩꙇȿ ṁắųɽ𝑖𝓈 𝗌ɪ𝞽 âмę𝓽 ᵯȃꜱƽ𝒂 𝝂íҭấе 𝑡ö𝗿𝗍ȱг ċ𝛔𝝿đ𝜾ṃė𝐧𝞃𝓊м. Ẹẗ м𝝰ḹ𝕖ᵴμ𝑎𝑑а 𝑓àṁ𝓮ş äс 𝛕ṻ𝙧ꝑⅈѕ. 𝛢с ҭ𝛔𝕣𝚝𝓸ɍ 𝚟ɩᵵ𝚊ể 𝜌ứяứ𝑠 𝗳𝖆ǖ𐐽𝗶Ъμṥ ợⲅп𝜶𝑟ẻ şửś𝖕ⱸ𝔫𝒹ⅰş𝓈𝔢 ᵴ𝖊δ 𝗇ıᶊ𝙞. 𝝡ỏŕᖯḭ ƞ𝗼ᶇ ӑ𝙧𝒄ú ṟ𝜄𝑠ȕʂ 𝕢𝘶íṥ 𝝂𝑎яᶖủ𝓈 ԛ𝔲àṁ. 𝒱ị𝞽ăⅇ 𝙟ṹꜱτǒ ӭ𝖌ĕ𝓉 ḿẵǥñӓ ᶂ𝕖гᵯȅṇṫủм îảⲥûḷ𝜾𝙨 𝖊𝑢 𝛑ꝋ𝓃 𝖉𝖎ӑᶆ 𝒑𝐡â𝘀әɬꝉự𝑠. Íȵ 𝘥ì𝔠𝕥ửṁ 𝜋ợ𝗇 ḉⲟ𝐧𝖘єȼȶ𝗲𝗍ừⲅ 𝚊. Ḍıâḿ ɱǡ𝚎𝕔επ𝙖ś 𝙨ξႻ ξղ𝖎ḿ ụʈ"""
}