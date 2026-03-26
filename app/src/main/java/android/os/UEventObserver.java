package android.os;

import java.util.ArrayList;
import java.util.HashMap;

//Please do not modify this class
public abstract class UEventObserver {
    private static final String TAG = "UEventObserver";
    private static final boolean DEBUG = false;
    private static UEventThread sThread;

    private static native void nativeSetup();

    private static native String nativeWaitForNextEvent();

    private static native void nativeAddMatch(String var0);

    private static native void nativeRemoveMatch(String var0);

    public UEventObserver() {
    }

    protected void finalize() throws Throwable {
        try {
            this.stopObserving();
        } finally {
            super.finalize();
        }

    }

    private static UEventThread getThread() {
        Class var0 = UEventObserver.class;
        synchronized(UEventObserver.class) {
            if (sThread == null) {
                sThread = new UEventThread();
                sThread.start();
            }

            return sThread;
        }
    }

    private static UEventThread peekThread() {
        Class var0 = UEventObserver.class;
        synchronized(UEventObserver.class) {
            return sThread;
        }
    }

    public final void startObserving(String var1) {
        if (var1 != null && !var1.isEmpty()) {
            UEventThread var2 = getThread();
            var2.addObserver(var1, this);
        } else {
            throw new IllegalArgumentException("match substring must be non-empty");
        }
    }

    public final void stopObserving() {
        UEventThread var1 = peekThread();
        if (var1 != null) {
            var1.removeObserver(this);
        }

    }

    public abstract void onUEvent(UEvent var1);

    private static final class UEventThread extends Thread {
        private final ArrayList<Object> mKeysAndObservers = new ArrayList();
        private final ArrayList<UEventObserver> mTempObserversToSignal = new ArrayList();

        public UEventThread() {
            super("UEventObserver");
        }

        public void run() {
            UEventObserver.nativeSetup();

            while(true) {
                String var1;
                do {
                    var1 = UEventObserver.nativeWaitForNextEvent();
                } while(var1 == null);

                this.sendEvent(var1);
            }
        }

        private void sendEvent(String var1) {
            int var3;
            int var4;
            synchronized(this.mKeysAndObservers) {
                var3 = this.mKeysAndObservers.size();

                for(var4 = 0; var4 < var3; var4 += 2) {
                    String var5 = (String)this.mKeysAndObservers.get(var4);
                    if (var1.contains(var5)) {
                        UEventObserver var6 = (UEventObserver)this.mKeysAndObservers.get(var4 + 1);
                        this.mTempObserversToSignal.add(var6);
                    }
                }
            }

            if (!this.mTempObserversToSignal.isEmpty()) {
                UEvent var2 = new UEvent(var1);
                var3 = this.mTempObserversToSignal.size();

                for(var4 = 0; var4 < var3; ++var4) {
                    UEventObserver var9 = (UEventObserver)this.mTempObserversToSignal.get(var4);
                    var9.onUEvent(var2);
                }

                this.mTempObserversToSignal.clear();
            }

        }

        public void addObserver(String var1, UEventObserver var2) {
            synchronized(this.mKeysAndObservers) {
                this.mKeysAndObservers.add(var1);
                this.mKeysAndObservers.add(var2);
                UEventObserver.nativeAddMatch(var1);
            }
        }

        public void removeObserver(UEventObserver var1) {
            synchronized(this.mKeysAndObservers) {
                int var3 = 0;

                while(var3 < this.mKeysAndObservers.size()) {
                    if (this.mKeysAndObservers.get(var3 + 1) == var1) {
                        this.mKeysAndObservers.remove(var3 + 1);
                        String var4 = (String)this.mKeysAndObservers.remove(var3);
                        UEventObserver.nativeRemoveMatch(var4);
                    } else {
                        var3 += 2;
                    }
                }

            }
        }
    }

    public static final class UEvent {
        private final HashMap<String, String> mMap = new HashMap();

        public UEvent(String var1) {
            int var2 = 0;

            int var5;
            for(int var3 = var1.length(); var2 < var3; var2 = var5 + 1) {
                int var4 = var1.indexOf(61, var2);
                var5 = var1.indexOf(0, var2);
                if (var5 < 0) {
                    break;
                }

                if (var4 > var2 && var4 < var5) {
                    this.mMap.put(var1.substring(var2, var4), var1.substring(var4 + 1, var5));
                }
            }

        }

        public String get(String var1) {
            return (String)this.mMap.get(var1);
        }

        public String get(String var1, String var2) {
            String var3 = (String)this.mMap.get(var1);
            return var3 == null ? var2 : var3;
        }

        public String toString() {
            return this.mMap.toString();
        }
    }
}
