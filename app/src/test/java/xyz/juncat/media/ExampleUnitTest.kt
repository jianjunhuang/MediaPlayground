package xyz.juncat.media

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val size = getTargetSize(640, 360)
        assertEquals(size.width, 640)
        assertEquals(size.height, 360)

        val size1 = getTargetSize(360, 640)
        assertEquals(size1.width, 360)
        assertEquals(size1.height, 640)

        val size2 = getTargetSize(1920, 1080)
        assertEquals(size2.width, 1280)
        assertEquals(size2.height, 720)

        val size3 = getTargetSize(1080, 1920)
        assertEquals(size3.height, 1280)
        assertEquals(size3.width, 720)


        val size4 = getTargetSize(1080, 1920, 90)
        assertEquals(size4.height, 720)
        assertEquals(size4.width, 1280)
    }

    private val TARGET_WIDTH = 1280
    private val TARGET_HEIGHT = 720
    fun getTargetSize(width: Int, height: Int, rotation: Int = 0): Size {
        return if (width > height) {
            if (width < TARGET_WIDTH) {
                Size(width, height)
            } else {
                Size(TARGET_WIDTH, (height * TARGET_WIDTH / width))
            }
        } else {
            if (height < TARGET_WIDTH) {
                Size(width, height)
            } else {
                Size((width * TARGET_WIDTH / height), TARGET_WIDTH)
            }
        }.let {
            if (rotation == 90 || rotation == 270) {
                Size(it.height, it.width)
            } else {
                it
            }
        }
    }

    class Size(val width: Int, val height: Int)
}