import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;

class OptimizationImpl extends optimizationPOA implements optimizationOperations {

    class SingleServer implements Comparator<SingleServer>{
        public Short ip;
        public int timeout;
        public IntHolder id;
        private long timeFromLastHello;

        public SingleServer(Short ip, int timeout, IntHolder id) {
            this.ip = ip;
            this.timeout = timeout;
            this.timeFromLastHello = System.currentTimeMillis();
            this.id = id;
        }


        public boolean isActive() {
            return System.currentTimeMillis() - timeFromLastHello < timeout;
        }
        public void activate() {
                this.timeFromLastHello = System.currentTimeMillis();
        }

        @Override
        public int compare(SingleServer o1, SingleServer o2) {
            return o1.ip - o2.ip;
        }
    }

    private ConcurrentHashMap<IntHolder, SingleServer> servers = new ConcurrentHashMap<>();
    private List<ArrayList<Short>> addressRange = Collections.synchronizedList(new ArrayList<>()); //not used


    @Override
    public void register(short ip, int timeout, IntHolder id) {
        if(servers!=null && id != null){
            if (!servers.containsKey(id)){
                id.value = ip;
                servers.put(id, new SingleServer(ip, timeout, id));
            }else{
                if(servers.get(id) != null){
                    servers.get(id).activate();
                }

            }
        }
    }


    @Override
    public void hello(int id) {
        if (servers.contains(id))
            servers.get(id).activate();
    }

    @Override
    public void best_range(rangeHolder r) {
        ArrayList<Short> tmpRange = new ArrayList<>();
        ArrayList<Short> maxRange = new ArrayList<>();
        for (SingleServer e: servers.values()){
            if(e.isActive()){
                tmpRange.add(Short.valueOf(e.ip));
            }
            else{

                addressRange.add(tmpRange);
                tmpRange = new ArrayList<>();
            }
        }
        int maxSize = 0;

        for(ArrayList<Short> oneList: addressRange) {
            if (maxSize < oneList.size()) {
                maxSize = oneList.size();
                maxRange = oneList;
            }
        }
        if (maxRange.size() != 0) {
            Short[] stockArr = new Short[maxRange.size()];
            stockArr = maxRange.toArray(stockArr);
            r.value = new range(stockArr[0], stockArr[stockArr.length-1]);
        }
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
