import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.omg.CORBA.IntHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableServer.POA;

import com.sun.corba.se.org.omg.CORBA.ORB;

class OptimizationImpl extends optimizationPOA implements optimizationOperations {

    class ServerItem {
        private short ip;
        private int id;
        private int timeout;
        private long lastHello;

        public ServerItem(int id, short ip, int timeout) {
            this.id = id;
            this.ip = ip;
            this.timeout = timeout;
        }

        public short getIp() {
            return ip;
        }

        public int getId() {
            return id;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public void hello() {
            lastHello = System.currentTimeMillis();
        }

        public boolean isActive() {
            return System.currentTimeMillis() - lastHello < timeout;
        }
    }

    class ServerItemIpComparator implements Comparator<ServerItem> {
        @Override
        public int compare(ServerItem o1, ServerItem o2) {
            return o1.getIp() - o2.getIp();
        }
    }

    static AtomicInteger idCount = new AtomicInteger(0);

    private ConcurrentHashMap<Integer, ServerItem> idServerMap = new ConcurrentHashMap<Integer, ServerItem>();
    private ConcurrentHashMap<Short, ServerItem> ipServerMap = new ConcurrentHashMap<Short, ServerItem>();
    private ConcurrentSkipListSet<ServerItem> serverList = new ConcurrentSkipListSet<ServerItem>(new ServerItemIpComparator());

    @Override
    public void register(short ip, int timeout, IntHolder id) {
        ServerItem serverItem = ipServerMap.get(ip);
        if (serverItem != null) {
            serverItem.setTimeout(timeout);
            id.value = serverItem.getId();
        } else {
            id.value = idCount.getAndIncrement();
            serverItem = new ServerItem(id.value, ip, timeout);
            serverItem.hello();
            ipServerMap.put(ip, serverItem);
            idServerMap.put(id.value, serverItem);
            serverList.add(serverItem);
        }
    }

    @Override
    public void hello(int id) {
        ServerItem serverItem = idServerMap.get(id);
        if (serverItem != null) {
            serverItem.hello();
        }
    }

    @Override
    public void best_range(rangeHolder r) {
        range bestRange = null, tmpRange = null;
        Iterator<ServerItem> it = serverList.iterator();
        while (it.hasNext()) {
            ServerItem sItem = it.next();
            if (tmpRange == null && sItem.isActive()) {
                tmpRange = new range(sItem.getIp(), sItem.getIp());
            } else if (tmpRange != null && sItem.isActive()) {
                if (sItem.getIp() - 1 == tmpRange.to) {
                    tmpRange.to += 1;
                } else {
                    tmpRange = new range(sItem.getIp(), sItem.getIp());
                }
            } else {
                tmpRange = null;
            }
            if (bestRange == null || tmpRange != null && tmpRange.to - tmpRange.from > bestRange.to - bestRange.from) {
                bestRange = tmpRange;
            }
        }
        r.value = bestRange;
    }
}

class Start {

    public static void main(String[] args) {
        try {
            org.omg.CORBA.ORB orb = ORB.init(args, null);
            POA rootpoa = (POA) orb.resolve_initial_references("RootPOA");
            rootpoa.the_POAManager().activate();

            OptimizationImpl optimizationImpl = new OptimizationImpl();
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(optimizationImpl);

            System.out.println(orb.object_to_string(ref));

            org.omg.CORBA.Object namingContextObj = orb.resolve_initial_references("NameService");
            NamingContext nCont = NamingContextHelper.narrow(namingContextObj);
            NameComponent[] path = { new NameComponent("Optymalizacja", "Object") };

            nCont.rebind(path, ref);
            orb.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
