package com.splunk.rum;

import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
class CarrierFinder {

    private final TelephonyManager telephonyManager;

    CarrierFinder(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    Carrier get(){
        Carrier.Builder builder = Carrier.builder();
        int id = telephonyManager.getSimCarrierId();
        builder.id(id);
        CharSequence name = telephonyManager.getSimCarrierIdName();
        if(name != null && validString(name.toString())){
            builder.name(name.toString());
        }
        String simOperator = telephonyManager.getSimOperator();
        if(validString(simOperator) && simOperator.length() >= 5){
            String countryCode = simOperator.substring(0,3);
            String networkCode = simOperator.substring(3);
            builder.mobileCountryCode(countryCode)
                    .mobileNetworkCode(networkCode);
        }
        String isoCountryCode = telephonyManager.getSimCountryIso();
        if(validString(isoCountryCode)){
            builder.isoCountryCode(isoCountryCode);
        }
        return builder.build();
    }

    private boolean validString(String str){
        return !(str == null || str.isEmpty());
    }

}
