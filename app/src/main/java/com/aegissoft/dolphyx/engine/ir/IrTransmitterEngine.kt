package com.aegissoft.dolphyx.engine.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import com.aegissoft.dolphyx.data.IrSignal

class IrTransmitterEngine(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.applicationContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    fun getCarrierFrequencies(): List<Pair<Int, Int>> {
        val mgr = irManager ?: return emptyList()
        if (!mgr.hasIrEmitter()) return emptyList()
        return mgr.carrierFrequencies?.map { range ->
            range.minFrequency to range.maxFrequency
        }.orEmpty()
    }

    fun transmit(signal: IrSignal): Result<String> {
        val mgr = irManager ?: return Result.failure(
            IllegalStateException("ConsumerIrManager unavailable on this device"),
        )
        if (!mgr.hasIrEmitter()) {
            return Result.failure(IllegalStateException("No hardware IR emitter detected"))
        }
        val pattern = IrProtocolEncoder.encodeSignal(signal) ?: return Result.failure(
            IllegalArgumentException("Unsupported or invalid IR signal: ${signal.sigType}/${signal.protocol}"),
        )
        return try {
            mgr.transmit(pattern.frequencyHz, pattern.patternMicros)
            Result.success("Transmitted ${signal.name} at ${pattern.frequencyHz} Hz (${pattern.patternMicros.size} pulses)")
        } catch (e: RuntimeException) {
            Result.failure(e)
        }
    }
}
