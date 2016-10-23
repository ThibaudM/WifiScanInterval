package fr.inria.tyrex.wifiscaninterval;

import android.os.Build;

import java.util.List;

class Utils {


    static double[] statsOnVector(List<Double> vector) {
        double average = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < vector.size(); i++) {
            if(vector.get(i) < min) min = vector.get(i);
            if(vector.get(i) > max) max = vector.get(i);
            average = (average * i + vector.get(i)) / (i+1);
        }
        return new double[] {average, min, max};
    }

    static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

}
