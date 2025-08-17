package com.lanmusicsync.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.util.Util
import kotlin.math.max
import kotlin.math.min

object AudioUtils {
    
    fun getOptimalBufferSize(sampleRate: Int, channelCount: Int): Int {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        // Use 2-4 times the minimum buffer size for better performance
        return max(minBufferSize * 3, Constants.AUDIO_CHUNK_SIZE * 4)
    }
    
    fun createAudioTrack(
        sampleRate: Int,
        channelCount: Int,
        bufferSize: Int
    ): AudioTrack {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
            
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
            
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }
    }
    
    fun calculateLatency(context: Context): Long {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputLatency = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 0
            val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 44100
            
            if (outputLatency > 0 && sampleRate > 0) {
                (outputLatency * 1000L) / sampleRate
            } else {
                100L // Default latency estimate
            }
        } else {
            100L // Default latency for older devices
        }
    }
    
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    fun calculateSyncAdjustment(
        currentPosition: Long,
        expectedPosition: Long,
        tolerance: Long = Constants.SYNC_TOLERANCE_MS
    ): Long {
        val difference = expectedPosition - currentPosition
        
        return when {
            kotlin.math.abs(difference) <= tolerance -> 0L
            difference > Constants.MAX_SYNC_ADJUSTMENT_MS -> Constants.MAX_SYNC_ADJUSTMENT_MS
            difference < -Constants.MAX_SYNC_ADJUSTMENT_MS -> -Constants.MAX_SYNC_ADJUSTMENT_MS
            else -> difference
        }
    }
    
    fun mixAudioSamples(samples1: ShortArray, samples2: ShortArray, mixRatio: Float = 0.5f): ShortArray {
        val result = ShortArray(min(samples1.size, samples2.size))
        
        for (i in result.indices) {
            val mixed = (samples1[i] * (1 - mixRatio) + samples2[i] * mixRatio).toInt()
            result[i] = max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), mixed)).toShort()
        }
        
        return result
    }
    
    fun applyVolumeToSamples(samples: ShortArray, volume: Float): ShortArray {
        val clampedVolume = max(0f, min(1f, volume))
        return samples.map { (it * clampedVolume).toInt().toShort() }.toShortArray()
    }
}

