package com.example.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleBooks {

    fun ensureSampleBooks(context: Context): List<File> {
        val dir = File(context.filesDir, "sample_books")
        if (!dir.exists()) dir.mkdirs()

        val txtFile = File(dir, "深空信标.txt")
        if (!txtFile.exists()) {
            txtFile.writeText(SAMPLE_TXT_CONTENT, Charsets.UTF_8)
        }

        val epubFile = File(dir, "微光寓言集.epub")
        if (!epubFile.exists()) {
            createSampleEpub(epubFile)
        }

        return listOf(txtFile, epubFile)
    }

    private fun createSampleEpub(file: File) {
        val zos = ZipOutputStream(FileOutputStream(file))

        // 1. mimetype (must be uncompressed according to EPUB spec)
        val mimeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val mimeEntry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimeBytes.size.toLong()
            compressedSize = mimeBytes.size.toLong()
            val crc = CRC32()
            crc.update(mimeBytes)
            setCrc(crc.value)
        }
        zos.putNextEntry(mimeEntry)
        zos.write(mimeBytes)
        zos.closeEntry()

        // 2. container.xml
        val containerXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                </rootfiles>
            </container>
        """.trimIndent()
        writeZipEntry(zos, "META-INF/container.xml", containerXml)

        // 3. content.opf
        val opfXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>微光寓言集</dc:title>
                    <dc:creator>腕上守夜人</dc:creator>
                    <dc:language>zh-CN</dc:language>
                </metadata>
                <manifest>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    <item id="ch1" href="Text/ch1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="ch2" href="Text/ch2.xhtml" media-type="application/xhtml+xml"/>
                    <item id="ch3" href="Text/ch3.xhtml" media-type="application/xhtml+xml"/>
                </manifest>
                <spine toc="ncx">
                    <itemref idref="ch1"/>
                    <itemref idref="ch2"/>
                    <itemref idref="ch3"/>
                </spine>
            </package>
        """.trimIndent()
        writeZipEntry(zos, "OEBPS/content.opf", opfXml)

        // 4. toc.ncx
        val ncxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                <head>
                    <meta name="dtb:uid" content="urn:uuid:wrist-reader-sample-01"/>
                </head>
                <docTitle><text>微光寓言集</text></docTitle>
                <navMap>
                    <navPoint id="np-1" playOrder="1">
                        <navLabel><text>第1话 机械候鸟的迁徙</text></navLabel>
                        <content src="Text/ch1.xhtml"/>
                    </navPoint>
                    <navPoint id="np-2" playOrder="2">
                        <navLabel><text>第2话 齿轮森林的钟表匠</text></navLabel>
                        <content src="Text/ch2.xhtml"/>
                    </navPoint>
                    <navPoint id="np-3" playOrder="3">
                        <navLabel><text>第3话 银河边缘的微光酒馆</text></navLabel>
                        <content src="Text/ch3.xhtml"/>
                    </navPoint>
                </navMap>
            </ncx>
        """.trimIndent()
        writeZipEntry(zos, "OEBPS/toc.ncx", ncxXml)

        // 5. Chapters
        val ch1Html = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>第1话 机械候鸟的迁徙</title></head>
            <body>
                <h1>第1话 机械候鸟的迁徙</h1>
                <p>在北境的废弃旧塔顶，第一只发条百灵鸟迎着晨雾展开了铜制羽翼。</p>
                <p>它的胸膛内跳动着一颗发条心脏，每次摆动都会发出微小却坚定的嘀嗒声。那是上一个纪元遗留下来的微型机械生命。</p>
                <p>每年极夜降临之前，成千上万只机械候鸟会从废弃的信号塔与风力发电机顶端腾空而起，向着赤道方向的太阳能热塔飞去。</p>
                <p>老技师倚靠在生锈的栏杆旁，默默转动手腕上的旧式发条腕表，静听天空中传来的清脆齿轮震颤。</p>
                <p>“只要发条还在转动，迁徙就不会停止。”他低语道，看着金色的小小影子划破灰霾的苍穹。</p>
            </body>
            </html>
        """.trimIndent()
        writeZipEntry(zos, "OEBPS/Text/ch1.xhtml", ch1Html)

        val ch2Html = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>第2话 齿轮森林的钟表匠</title></head>
            <body>
                <h1>第2话 齿轮森林的钟表匠</h1>
                <p>在地下城的最底层，有一座被黄铜齿轮与铜管缠绕的钟表工坊。</p>
                <p>钟表匠卡尔已经在这里工作了四十年。他的右眼装着一枚三倍放大的黄铜目镜，手指永远沾着润滑油与石英粉末。</p>
                <p>城里的居民都戴着他打造的手表。那些手表不需要联网，不需要充电，仅依靠佩戴者手腕的日常摆动来储存能量。</p>
                <p>“手表的精妙之处，”卡尔常对年轻学徒说，“就在于它必须贴着你的脉搏。每一秒的走动，都是你活着的时间印记。”</p>
                <p>当最深层的巨型蒸汽钟敲响午夜十二下，整座齿轮森林都在金属共振中轻轻低鸣。</p>
            </body>
            </html>
        """.trimIndent()
        writeZipEntry(zos, "OEBPS/Text/ch2.xhtml", ch2Html)

        val ch3Html = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>第3话 银河边缘的微光酒馆</title></head>
            <body>
                <h1>第3话 银河边缘的微光酒馆</h1>
                <p>猎户座旋臂的外缘，漂浮着一座由老旧矿业运输船改装的空间补给站，名叫“微光酒馆”。</p>
                <p>这里没有喧闹的星际广播，只有一台播放着模拟黑胶唱片的复古留声机。</p>
                <p>漂泊了数十个光年的货运飞船船长们推开舱门，卸下厚重的增压宇航服，围坐在全息壁炉旁小酌合成麦芽酒。</p>
                <p>酒保是一位老旧的仿生人，动作沉稳，总会在客人杯底垫上一张印有恒星坐标的纸质便签。</p>
                <p>“无论航行到宇宙多么荒凉的角落，”酒保微笑着说，“只要回头望向舷窗，群星总有一颗在为你闪烁。”</p>
            </body>
            </html>
        """.trimIndent()
        writeZipEntry(zos, "OEBPS/Text/ch3.xhtml", ch3Html)

        zos.close()
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private const val SAMPLE_TXT_CONTENT = """深空信标 (星际短篇选集)
作者：观测者零号

第壹章 深空折跃
　　“跃迁引擎充能倒计时，三、二、一——”
　　随着领航员平静的汇报声落下，巨大的引力透镜在舷窗外瞬间收缩，群星被拉扯成一道道幽蓝的流光。
　　这是远征舰“寻星号”离开太阳系边界的第三百个标准日。
　　船员们已经习惯了这种超光速穿梭带来的轻微失重眩晕。在狭小的睡眠舱里，年轻的通讯员握着一枚机械怀表。表盘上指针跳动的频率，与地球老家海边灯塔的闪烁节奏完全一致。
　　“雷达探测到未知波段的谐振信号！”控制台忽然亮起琥珀色的警告灯。
　　那是一段极为规整的脉冲信号，穿透了狂暴的柯伊伯带暗物质乱流，直接投射在主屏幕的中央。

第贰章 赛博观测站
　　在冰封的小行星表面，半埋于黑色冻土之中的是一座呈八角环形的自动观测站。
　　这里没有活人，只有中央AI“泰弥斯”维持着长达三千年的冷寂值守。
　　它的逻辑核心每隔八个小时进行一次自检，扫描周围三万公里的射电频谱，然后将数据刻录在石英微晶晶片上。
　　漫长的岁月里，偶有流星掠过荒芜的平原，撞击出深蓝色的尘埃雾。
　　今天，“泰弥斯”的传感器忽然捕捉到一声人类语言的呼叫。
　　“你好，这里是寻星号，是否有人在站？”
　　休眠已久的指令序列在毫秒内苏醒，古老的继电器发出清脆的接通声，如同沉睡巨兽的一声叹息。

第叁章 硅基低语
　　当两台相距万里的机器建立通讯连接时，信息是以太空中最纯粹的光脉冲交换的。
　　“泰弥斯”向寻星号传输了三千年间记录的星云演化图谱。那些超新星爆发的绚烂瞬间，全部被压缩在微小的光斑里。
　　“你们迟到了四百个标准年，”AI的声音透过舱内扬声器响起，虽然带有金属质感的机械杂音，却显得格外温和。
　　“但这颗星球的地热能源还在运转，温室里的耐寒地衣已经开出了白色的小花。”
　　舰长凝视着舷窗下方那片白茫茫的星球表面，眼角微润：“我们终于找到了落脚点。”

第肆章 恒星余晖
　　探险队的着陆舱在小行星赤道平稳着陆。
　　当减压舱门缓缓开启，久违的微风带着泥土与臭氧的熟悉气息拂面而来。
　　通讯员抬起手腕，低头看了一眼手表。手表的秒针依旧在精准地划过刻度。在距离故乡几百光年的星空深处，时间依然在忠诚地陪伴着这群孤独的旅人。
　　远处的橙色恒星正在地平线尽头缓缓升起，将金属塔身镀上了一层耀眼的黄金。
　　新的纪元，就在这枚小小的手腕方寸之间，悄然翻开了崭新的一页。
"""
}
