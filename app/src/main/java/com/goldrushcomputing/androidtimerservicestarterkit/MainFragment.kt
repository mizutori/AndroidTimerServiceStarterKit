package com.goldrushcomputing.androidtimerservicestarterkit


import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.goldrushcomputing.androidtimerservicestarterkit.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding


    var timerService: TimerService? = null
    var isServiceBound = false

    private var timerUpdateReceiver: BroadcastReceiver? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMainBinding.inflate(inflater, container, false)

        with(binding) {

            startButton.isEnabled = false
            stopButton.isEnabled = false


            startButton.setOnClickListener {
                startButton.isEnabled = false
                stopButton.isEnabled = true

                val timerValueInSeconds = 30 * 60
                val minutes = timerValueInSeconds / 60
                val seconds = timerValueInSeconds % 60
                binding.timerText.text = String.format(getString(R.string.timer_display_format), minutes, seconds)

                timerService?.startTimer(timerValueInSeconds.toLong())
            }

            stopButton.setOnClickListener {
                startButton.isEnabled = true
                stopButton.isEnabled = false

                timerService?.stopTimer()

            }
        }

        timerUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val remainingTimeInSeconds = intent.getLongExtra("remainingTimeInSeconds", 0)
                val minutes = remainingTimeInSeconds / 60
                val seconds = remainingTimeInSeconds % 60

                binding.timerText.text = String.format(getString(R.string.timer_display_format), minutes, seconds)

            }
        }

        activity?.let {activity ->
            timerUpdateReceiver?.let{
                LocalBroadcastManager.getInstance(activity).registerReceiver(
                    it,
                    IntentFilter("TimerUpdated")
                )
            }
        }


        startService()



        return binding.root
    }


    private fun startService() {
        this.activity?.let{activity ->
            val timerService = Intent(activity.application, TimerService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.application.startForegroundService(timerService)
            } else {
                activity.application.startService(timerService)
            }
            isServiceBound = activity.application.bindService(timerService, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun stopSerivce() {
        timerService?.let{
            if(isServiceBound){

                activity?.application?.unbindService(serviceConnection)
                isServiceBound = false


                it.stopForeground(true)
                it.stopSelf()
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            val name = className.className

            if (name.endsWith("TimerService")) {
                timerService = (service as TimerService.TimerServiceBinder).service

                timerService?.timer?.let{
                    binding.startButton.isEnabled = false //Enable Start Button when timer service is ready
                    binding.stopButton.isEnabled = true
                } ?: run{
                    binding.startButton.isEnabled = true
                    binding.stopButton.isEnabled = false
                }

            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            if (className.className == "TimerService") {
                timerService = null
                binding.startButton.isEnabled = false //Disable Start Button when timer service is dead
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            activity?.let {activity ->
                timerUpdateReceiver?.let{
                    LocalBroadcastManager.getInstance(activity).unregisterReceiver(it)
                }
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
    }

}
