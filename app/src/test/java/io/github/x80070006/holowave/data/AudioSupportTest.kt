package io.github.x80070006.holowave.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSupportTest {

    @Test fun commonFormatsArePlayable() {
        listOf("audio/mpeg", "audio/mp4", "audio/flac", "audio/ogg", "audio/x-wav", "audio/x-matroska", "audio/aac-adts", "audio/amr", "video/webm", "video/3gpp")
            .forEach { assertTrue(it, AudioSupport.isPlayable(it)) }
    }

    @Test fun containersNoDecoderCanReadAreFlagged() {
        listOf("audio/x-ms-wma", "audio/x-aiff", "AUDIO/AIFF", "audio/x-ape").forEach { assertFalse(it, AudioSupport.isPlayable(it)) }
    }

    @Test fun unknownTypeGetsTheBenefitOfTheDoubt() {
        assertTrue(AudioSupport.isPlayable(""))
        assertTrue(AudioSupport.isPlayable(null))
    }
}
