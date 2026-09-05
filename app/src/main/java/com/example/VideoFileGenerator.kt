package com.example

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File

object VideoFileGenerator {
    private const val TAG = "VideoFileGenerator"
    private const val IFRAME_INTERVAL = 1

    /**
     * Checks if HEVC hardware or software encoder is available on the device.
     */
    fun isHevcEncoderSupported(): Boolean {
        try {
            val count = android.media.MediaCodecList.getCodecCount()
            for (i in 0 until count) {
                val codecInfo = android.media.MediaCodecList.getCodecInfoAt(i)
                if (codecInfo.isEncoder) {
                    val types = codecInfo.supportedTypes
                    if (types != null) {
                        for (type in types) {
                            if (type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)) {
                                return true
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            // Safe fallback
        }
        return false
    }

    /**
     * Generates a fully playable custom MP4 video of the specified duration.
     * Captures the composition layout, aspect ratios, and simulated visual telemetry grids.
     */
    fun generateVideo(
        outputFile: File,
        durationSeconds: Int,
        width: Int,
        height: Int,
        layout: String, // PIP, SPLIT_HORIZONTAL, etc.
        isConcurrent: Boolean,
        frameRate: Int = 30,
        useHevc: Boolean = false,
        audioFilePath: String? = null,
        isStabilizationActive: Boolean = false,
        stabilizationStrength: String = "Standard",
        watermark: WatermarkConfig? = null,
        mismatchMode: String = "LETTERBOX",
        originalAspectRatio: String = "16:9",
        isWhatsApp: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var audioExtractor: android.media.MediaExtractor? = null

        var watermarkLogoBitmap: android.graphics.Bitmap? = null
        if (watermark != null && watermark.type == "IMAGE" && watermark.imagePath != null) {
            try {
                val f = java.io.File(watermark.imagePath)
                if (f.exists()) {
                    watermarkLogoBitmap = android.graphics.BitmapFactory.decodeFile(watermark.imagePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode watermark logo bitmap: ${watermark.imagePath}", e)
            }
        }
        
        // Ensure dimensions are multiples of 16 for standard H.264 compatibilities
        val w = (width / 16) * 16
        val h = (height / 16) * 16
        
        var selectedMime = if (useHevc && isHevcEncoderSupported()) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
        
        try {
            // Attempt to load and read the audio format beforehand if provided
            var audioFormat: MediaFormat? = null
            var audioSourceTrackIndex = -1
            var audioTrackIndex = -1
            
            if (audioFilePath != null && java.io.File(audioFilePath).exists()) {
                try {
                    audioExtractor = android.media.MediaExtractor()
                    audioExtractor.setDataSource(audioFilePath)
                    for (i in 0 until audioExtractor.trackCount) {
                        val format = audioExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            audioFormat = format
                            audioSourceTrackIndex = i
                            break
                        }
                    }
                } catch (ae: Exception) {
                    Log.e(TAG, "Failed to read audio file format details: $audioFilePath", ae)
                    audioExtractor?.release()
                    audioExtractor = null
                }
            }

            val numFrames = (durationSeconds * frameRate).coerceAtLeast(frameRate)
            val format = MediaFormat.createVideoFormat(selectedMime, w, h).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                
                val baseBitrate = when (frameRate) {
                    24 -> 1200000
                    60 -> 2500000
                    else -> 1500000
                }
                var finalBitrate = if (selectedMime == MediaFormat.MIMETYPE_VIDEO_HEVC) (baseBitrate * 0.6f).toInt() else baseBitrate
                if (isWhatsApp) {
                    val durationSecs = durationSeconds.coerceAtLeast(1)
                    val maxBps = ((14.0 * 1024.0 * 1024.0 * 8.0) / durationSecs).toInt()
                    finalBitrate = finalBitrate.coerceAtMost(maxBps).coerceAtLeast(100000)
                }
                setInteger(MediaFormat.KEY_BIT_RATE, finalBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
            }

            try {
                mediaCodec = MediaCodec.createEncoderByType(selectedMime)
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (t: Throwable) {
                if (selectedMime == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                    Log.w(TAG, "HEVC encoder creation/configure failed, falling back to AVC", t)
                    selectedMime = MediaFormat.MIMETYPE_VIDEO_AVC
                    val formatAvc = MediaFormat.createVideoFormat(selectedMime, w, h).apply {
                        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                        val baseBitrate = when (frameRate) {
                            24 -> 1200000
                            60 -> 2500000
                            else -> 1500000
                        }
                        setInteger(MediaFormat.KEY_BIT_RATE, baseBitrate)
                        setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
                    }
                    mediaCodec = MediaCodec.createEncoderByType(selectedMime)
                    mediaCodec.configure(formatAvc, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                } else {
                    throw t
                }
            }
            mediaCodec.start()

            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val yuvFrameSize = w * h * 3 / 2
            val yuvBuffer = ByteArray(yuvFrameSize)

            var frameCount = 0
            var isEOS = false

            while (!isEOS) {
                // Handle input buffer
                if (frameCount < numFrames) {
                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            
                            // Generate custom YUV frame representing what's seen on screen!
                            generateYuvFrame(
                                yuv = yuvBuffer,
                                w = w,
                                h = h,
                                frameIndex = frameCount,
                                totalFrames = numFrames,
                                layout = layout,
                                isConcurrent = isConcurrent,
                                isStabilizationActive = isStabilizationActive,
                                stabilizationStrength = stabilizationStrength,
                                mismatchMode = mismatchMode,
                                originalAspectRatio = originalAspectRatio
                            )
                            
                            // Apply custom burned-in watermark overlay if present!
                            if (watermark != null) {
                                applyWatermarkYuv(yuvBuffer, w, h, frameCount, frameRate, watermark, watermarkLogoBitmap)
                            }
                            
                            inputBuffer.put(yuvBuffer)
                            val ptsUsec = (frameCount * 1000000L) / frameRate
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuvFrameSize, ptsUsec, 0)
                            frameCount++
                            onProgress(frameCount.toFloat() / numFrames)
                        }
                    }
                } else {
                    // Send EOS
                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    }
                }

                // Handle output buffer
                var outBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outBufferIndex >= 0) {
                    if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice unexpectedly")
                        }
                        val newFormat = mediaCodec.outputFormat
                        trackIndex = mediaMuxer.addTrack(newFormat)
                        
                        // Add audio track to muxer if format exists
                        if (audioFormat != null) {
                            audioTrackIndex = mediaMuxer.addTrack(audioFormat)
                        }
                        
                        mediaMuxer.start()
                        muxerStarted = true
                    } else if (outBufferIndex >= 0) {
                        val encodedData = mediaCodec.getOutputBuffer(outBufferIndex)
                        if (encodedData != null) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0) {
                                if (!muxerStarted) {
                                    throw RuntimeException("Muxer not started properly")
                                }
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            mediaCodec.releaseOutputBuffer(outBufferIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                isEOS = true
                                break
                            }
                        }
                    }
                    outBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            // Mux audio track content if audio track is active
            if (audioTrackIndex >= 0 && audioExtractor != null) {
                try {
                    val byteBuffer = java.nio.ByteBuffer.allocate(1024 * 256)
                    val audioBufferInfo = MediaCodec.BufferInfo()
                    audioExtractor.selectTrack(audioSourceTrackIndex)
                    while (true) {
                        val sampleSize = audioExtractor.readSampleData(byteBuffer, 0)
                        if (sampleSize < 0) {
                            break
                        }
                        audioBufferInfo.offset = 0
                        audioBufferInfo.size = sampleSize
                        audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        val isSync = (audioExtractor.sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                        audioBufferInfo.flags = if (isSync) android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, audioBufferInfo)
                        audioExtractor.advance()
                    }
                } catch (ae: Exception) {
                    Log.e(TAG, "Error writing audio track details to final muxed MP4", ae)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating dynamic layout video", e)
            return false
        } finally {
            try {
                audioExtractor?.release()
            } catch (ignored: Exception) {}
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (ignored: Exception) {}
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.stop()
                    mediaMuxer.release()
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun generateYuvFrame(
        yuv: ByteArray,
        w: Int,
        h: Int,
        frameIndex: Int,
        totalFrames: Int,
        layout: String,
        isConcurrent: Boolean,
        isStabilizationActive: Boolean = false,
        stabilizationStrength: String = "Standard",
        mismatchMode: String = "LETTERBOX",
        originalAspectRatio: String = "16:9"
    ) {
        val ySize = w * h
        val uvOffset = ySize
        val frameFactor = frameIndex.toFloat() / totalFrames

        // Phase angle for smooth animated effects (oscilloscoping lines, targets, scanner wave)
        val phase = (frameIndex * 2 * Math.PI / 30).toFloat()

        // EIS simulated crop zoom level
        val scale = if (!isStabilizationActive) 1.0f
        else when (stabilizationStrength) {
            "Light" -> 1.04f
            "Standard" -> 1.10f
            "High" -> 1.20f
            else -> 1.0f
        }
        val cx = w / 2f
        val cy = h / 2f

        // EIS handheld jitter simulation (shaky camera gets butter-smooth when active!)
        var shakeX = 0f
        var shakeY = 0f
        val maxShake = if (!isStabilizationActive) {
            12f
        } else {
            when (stabilizationStrength) {
                "Light" -> 6f
                "Standard" -> 2f
                "High" -> 0f
                else -> 12f
            }
        }
        
        if (maxShake > 0f) {
            val time = frameIndex * 0.45f
            shakeX = (Math.sin(time.toDouble()) * 0.6f + Math.cos((time * 1.6f).toDouble()) * 0.4f).toFloat() * maxShake
            shakeY = (Math.cos((time * 0.9f).toDouble()) * 0.5f + Math.sin((time * 1.4f).toDouble()) * 0.5f).toFloat() * maxShake
        }

        // Mismatch Aspect Ratio Logic
        val r_target = w.toFloat() / h
        val r_orig = when (originalAspectRatio) {
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "1:1" -> 1f
            "4:5" -> 0.8f
            else -> 16f / 9f
        }
        val hasMismatch = Math.abs(r_orig - r_target) > 0.05f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = y * w + x
                
                var sx = x.toFloat()
                var sy = y.toFloat()
                var isBar = false

                if (hasMismatch) {
                    val modeUpper = mismatchMode.uppercase()
                    if (modeUpper == "CROP") {
                        if (r_orig > r_target) {
                            // original is wider than target: zoom x to crop sides
                            val zoomFactor = r_target / r_orig
                            sx = cx + (x - cx) / zoomFactor
                        } else {
                            // original is taller than target: zoom y to crop top/bottom
                            val zoomFactor = r_orig / r_target
                            sy = cy + (y - cy) / zoomFactor
                        }
                    } else if (modeUpper == "LETTERBOX" || modeUpper == "ADD BARS" || modeUpper == "ADD_BARS") {
                        if (r_orig > r_target) {
                            // wider original in taller target: top & bottom bars
                            val videoH = w / r_orig
                            val offsetY = (h - videoH) / 2f
                            if (y < offsetY || y >= h - offsetY) {
                                isBar = true
                            } else {
                                sy = (y - offsetY) * (h.toFloat() / videoH)
                            }
                        } else {
                            // taller original in wider target: left & right side bars
                            val videoW = h * r_orig
                            val offsetX = (w - videoW) / 2f
                            if (x < offsetX || x >= w - offsetX) {
                                isBar = true
                            } else {
                                sx = (x - offsetX) * (w.toFloat() / videoW)
                            }
                        }
                    }
                }

                if (isBar) {
                    yuv[index] = 16.toByte() // standard Y black level representation
                    if (y % 2 == 0 && x % 2 == 0) {
                        val uvIndex = uvOffset + (y / 2) * (w / 2) + (x / 2) * 2
                        if (uvIndex < yuv.size - 1) {
                            yuv[uvIndex] = 128.toByte()   // neutral chroma
                            yuv[uvIndex + 1] = 128.toByte()
                        }
                    }
                    continue
                }

                // Mapped coordinates applying dynamic scale (EIS sensor crop) & hand shake
                val mappedX = (((sx - cx) / scale) + cx + shakeX).toInt().coerceIn(0, w - 1)
                val mappedY = (((sy - cy) / scale) + cy + shakeY).toInt().coerceIn(0, h - 1)
                
                // Segment pixels into "Feed A" (Background/Back lens) or "Feed B" (Foreground/Front lens/Simulation) using mapped coordinates
                var isFeedB = false

                when (layout) {
                    "PIP" -> {
                        // Picture-in-Picture float panel in the bottom-right quadrant
                        val pipW = (w * 0.35f).toInt()
                        val pipH = (h * 0.35f).toInt()
                        val pipX = (w * 0.60f).toInt()
                        val pipY = (h * 0.60f).toInt()
                        if (mappedX in pipX until (pipX + pipW) && mappedY in pipY until (pipY + pipH)) {
                            // Check thin pip border
                            if (mappedX == pipX || mappedX == pipX + pipW - 1 || mappedY == pipY || mappedY == pipY + pipH - 1) {
                                yuv[index] = 230.toByte() // Bright outline
                                continue
                            }
                            isFeedB = true
                        }
                    }
                    "SPLIT_HORIZONTAL" -> {
                        // Horizontal divider in the center
                        val dividerY = h / 2
                        if (Math.abs(mappedY - dividerY) < 3) {
                            yuv[index] = 220.toByte()
                            continue
                        }
                        if (mappedY > dividerY) {
                            isFeedB = true
                        }
                    }
                    "SPLIT_VERTICAL" -> {
                        // Vertical divider in the center
                        val dividerX = w / 2
                        if (Math.abs(mappedX - dividerX) < 3) {
                            yuv[index] = 220.toByte()
                            continue
                        }
                        if (mappedX > dividerX) {
                            isFeedB = true
                        }
                    }
                    "SPLIT_DIAGONAL" -> {
                        // Diagonal split screen line: y = (h / w) * x
                        val expectedY = (h.toFloat() / w.toFloat()) * mappedX
                        if (Math.abs(mappedY - expectedY) < 4) {
                            yuv[index] = 220.toByte()
                            continue
                        }
                        if (mappedY > expectedY) {
                            isFeedB = true
                        }
                    }
                }

                // Render colors based on feed selection:
                // Feed A uses an elegant deep cosmic color gradient (pulsing dark ambient tones with camera grids/overlays)
                // Feed B uses a contrasting copper/red technical graphic tone
                if (isFeedB) {
                    // Feed B Design
                    // Give it a subtle panning radial gradient
                    val centerX = w * 0.7f + Math.cos(phase.toDouble()).toFloat() * 15
                    val centerY = h * 0.5f + Math.sin(phase.toDouble()).toFloat() * 15
                    val dx = mappedX - centerX
                    val dy = mappedY - centerY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    
                    // Draw nice dynamic rings
                    val ring = (Math.sin((dist / 35.0f - phase).toDouble()).toFloat() + 1.0f) / 2.0f
                    
                    val brightness = (90 + ring * 120).toInt().coerceIn(0, 255)
                    yuv[index] = brightness.toByte()

                    // UV Chroma elements: Classic copper hues (orange-red tint)
                    if (y % 2 == 0 && x % 2 == 0) {
                        val uvIndex = uvOffset + (y / 2) * (w / 2) + (x / 2) * 2
                        if (uvIndex < yuv.size - 1) {
                            yuv[uvIndex] = 95.toByte()   // U
                            yuv[uvIndex + 1] = 175.toByte() // V
                        }
                    }
                } else {
                    // Feed A Design
                    // Generate nice futuristic vertical sweep line
                    val sweepY = (h * frameFactor).toInt()
                    val distToSweep = Math.abs(mappedY - sweepY)
                    
                    val isGridLine = (mappedX % 80 == 0 || mappedY % 80 == 0)
                    var luma = if (isGridLine) 85 else 35
                    
                    // Sweep line glows green
                    if (distToSweep < 6) {
                        luma = 220
                    }
                    
                    yuv[index] = luma.toByte()

                    // UV Chroma elements: Deep glowing green/blue hue
                    if (y % 2 == 0 && x % 2 == 0) {
                        val uvIndex = uvOffset + (y / 2) * (w / 2) + (x / 2) * 2
                        if (uvIndex < yuv.size - 1) {
                            if (distToSweep < 6) {
                                yuv[uvIndex] = 110.toByte()   // U
                                yuv[uvIndex + 1] = 85.toByte()  // V (cyan-green bias)
                            } else {
                                yuv[uvIndex] = 135.toByte()   // Balanced cinematic grey tone
                                yuv[uvIndex + 1] = 115.toByte()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyWatermarkYuv(
        yuv: ByteArray,
        w: Int,
        h: Int,
        frameIndex: Int,
        frameRate: Int,
        watermark: WatermarkConfig,
        cachedLogo: android.graphics.Bitmap?
    ) {
        val ySize = w * h
        val uvOffset = ySize

        // Generate dynamic text for auto-date or text watermark
        val dateString = if (watermark.isAutoTime) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            val startTime = System.currentTimeMillis() - (15 * 1000)
            val currentTimeMs = startTime + (frameIndex * 1000L / frameRate)
            sdf.format(java.util.Date(currentTimeMs))
        } else ""

        val displayText = if (watermark.type == "TEXT") {
            if (watermark.isAutoTime) {
                if (watermark.text.isNotEmpty()) "${watermark.text} $dateString" else dateString
            } else {
                watermark.text
            }
        } else if (watermark.isAutoTime) {
            dateString
        } else ""

        var watermarkBmp: android.graphics.Bitmap? = null

        try {
            if (watermark.type == "IMAGE" && cachedLogo != null) {
                val targetW = (w * 0.22f * watermark.scale).toInt().coerceIn(32, w / 2)
                val aspect = cachedLogo.height.toFloat() / cachedLogo.width.toFloat()
                val targetH = (targetW * aspect).toInt().coerceIn(16, h / 2)
                watermarkBmp = android.graphics.Bitmap.createScaledBitmap(cachedLogo, targetW, targetH, true)
            } else if (displayText.isNotEmpty()) {
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor(watermark.color)
                    val baseTextSize = watermark.fontSize.toFloat().coerceIn(12f, 96f)
                    textSize = baseTextSize * (h / 720f) * watermark.scale
                    typeface = when (watermark.font) {
                        "Monospace" -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                        "Serif" -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                        "Sans-Serif" -> android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                        else -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
                    }
                }

                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(displayText, 0, displayText.length, textBounds)
                val textW = textBounds.width().coerceAtLeast(1)
                val textH = textBounds.height().coerceAtLeast(1)

                val bmpW = textW + 16
                val bmpH = textH + 16
                val localBmp = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(localBmp)
                canvas.drawText(displayText, 8f, textH.toFloat() + 4f, textPaint)
                watermarkBmp = localBmp
            }

            if (watermarkBmp == null) return

            val bmpW = watermarkBmp.width
            val bmpH = watermarkBmp.height

            val startX = (watermark.xPercent * w).toInt().coerceIn(0, (w - bmpW).coerceAtLeast(0))
            val startY = (watermark.yPercent * h).toInt().coerceIn(0, (h - bmpH).coerceAtLeast(0))

            val pixels = IntArray(bmpW * bmpH)
            watermarkBmp.getPixels(pixels, 0, bmpW, 0, 0, bmpW, bmpH)

            for (localY in 0 until bmpH) {
                val globalY = startY + localY
                if (globalY >= h) break
                for (localX in 0 until bmpW) {
                    val globalX = startX + localX
                    if (globalX >= w) break

                    val pixel = pixels[localY * bmpW + localX]
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha == 0) continue

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    val pixelOpacity = (alpha / 255f) * watermark.opacity
                    if (pixelOpacity <= 0f) continue

                    val overlayY = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

                    val indexY = globalY * w + globalX
                    if (indexY < ySize) {
                        val originalY = yuv[indexY].toInt() and 0xFF
                        yuv[indexY] = (originalY * (1f - pixelOpacity) + overlayY * pixelOpacity).toInt().toByte()
                    }

                    if (globalY % 2 == 0 && globalX % 2 == 0) {
                        val uvIndex = uvOffset + (globalY / 2) * (w / 2) + (globalX / 2) * 2
                        if (uvIndex < yuv.size - 1) {
                            val overlayU = (-0.169f * r - 0.331f * g + 0.5f * b + 128f).toInt().coerceIn(0, 255)
                            val overlayV = (0.5f * r - 0.419f * g - 0.081f * b + 128f).toInt().coerceIn(0, 255)

                            val originalU = yuv[uvIndex].toInt() and 0xFF
                            val originalV = yuv[uvIndex + 1].toInt() and 0xFF

                            yuv[uvIndex] = (originalU * (1f - pixelOpacity) + overlayU * pixelOpacity).toInt().toByte()
                            yuv[uvIndex + 1] = (originalV * (1f - pixelOpacity) + overlayV * pixelOpacity).toInt().toByte()
                        }
                    }
                }
            }

            watermarkBmp.recycle()

        } catch (e: Exception) {
            Log.e("VideoFileGenerator", "Error drawing watermark or timestamp during video export", e)
        }
    }
}
