package cool.thejiangbf.sakula

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {
    var running:Boolean = false
    private lateinit var audioTrack:AudioTrack
    private lateinit var thread:PlayThread

    private var playing = false;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStop.setOnClickListener {
            playing = false
        }

        btnRandom.setOnClickListener {
            playing = true
            GlobalScope.launch {
                while (playing){
                    //随机
                    val r = Random(System.currentTimeMillis()).nextInt(20,400)
                    Log.i("TAG", "Random: r=$r")

                    //播放
                    delay(seek3.progress.toLong())
                    thread = PlayThread(r * 10)
                    thread.setChannel()
                    thread.start()
                    delay(seek2.progress.toLong())

                    //停止
                    thread.stopPlay()

                }
            }

        }

        seek2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                tvLength.text = "音节间隔(毫秒): $p1"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(p0: SeekBar?) { }

        })
        seek3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                tvDelay.text = "音节间隔(毫秒): $p1"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(p0: SeekBar?) { }

        })


    }

    fun playSound(left: Boolean,right: Boolean,rate:Int): Unit {
        thread = PlayThread(rate)
        thread.setChannel(left, right)
        thread.start()
    }

    fun start(){
        running = true

        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setHapticChannelsMuted(true)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(44100)
            .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
            .build()

        audioTrack = AudioTrack(attributes,format,1,AudioTrack.MODE_STREAM,1)

        audioTrack.play()

    }


    fun stop(): Unit {
        running = false
        audioTrack.stop()
        audioTrack.release()

    }

    object Wave{
        private const val HEIGHT = 128
        const val _2PI = PI*2

        fun sinWave(wave:ByteArray,waveLen:Int,len:Int) : ByteArray{
            for (i in 0 until len){
                wave[i] = (HEIGHT * (1 - sin(_2PI*((i%waveLen) * 1.0 / waveLen)))).toInt().toByte()
            }
            return wave
        }


    }

    class PlayThread(private val rate: Int) : Thread() {
        private val RATE = 44100
        var mAudioTrack: AudioTrack? = null
        var playing = false

        /**
         * 总长度
         */
        var length = 0

        /**
         * 一个正弦波的长度
         */
        var waveLen = 0

        /**
         * 频率
         */
        var Hz = 0

        /**
         * 正弦波
         */
        lateinit var wave: ByteArray

        init {
            if (rate > 0) {
                Hz = rate
                waveLen = RATE / Hz
                length = waveLen * Hz
                wave = ByteArray(RATE)
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,  // CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM
                )
                playing = true
                wave = Wave.sinWave(wave, waveLen, length)
            }
        }


        override fun run() {
            super.run()
            if (null != mAudioTrack) mAudioTrack!!.play()
            //一直播放
            while (playing) {
                mAudioTrack!!.write(wave, 0, length)
            }
        }

        /**
         * 设置左右声道，左声道时设置右声道音量为0，右声道设置左声道音量为0
         *
         * @param left  左声道
         * @param right 右声道
         */
        fun setChannel(left: Boolean = true, right: Boolean = true) {
            if (null != mAudioTrack) {
                mAudioTrack!!.setStereoVolume(
                   if (left) 1f else 0f,
                   if (right) 1f else 0f,
                )
            }
        }

        //设置音量
        fun setVolume(left: Float, right: Float) {
            if (null != mAudioTrack) {
                mAudioTrack!!.setStereoVolume(left, right)
            }
        }

        fun stopPlay() {
            playing = false
            releaseAudioTrack()
        }

        private fun releaseAudioTrack() {
            if (null != mAudioTrack) {
                mAudioTrack!!.stop()
                mAudioTrack!!.release()
                mAudioTrack = null
            }
        }

    }

}