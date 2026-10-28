package tankops.server.game;
import java.util.*;
import java.lang.reflect.*;
import tankops.common.map.MapLoader;
import tankops.common.model.common.Direction;
import tankops.common.net.codec.MessageCodec;

import tankops.server.net.ClientSession;
public class TickRateExperiment {
    static GameRoom newRoom(int players, int rate) throws Exception {
        GameRoom r = new GameRoom("room-1", 5);
        for (int i = 0; i < players; i++) r.addPlayer(new ClientSession(null, null));
        for (int i = 1; i < players; i++) r.setPlayerReady(r.getPlayers().get(i).getPlayerName(), true);
        r.startGame(new MapLoader().loadMap("/maps/map_multi.map"), rate);
        return r;
    }
    public static void main(String[] a) throws Exception {
        int players = 5;
        Method tick = GameRoom.class.getDeclaredMethod("tick"); tick.setAccessible(true);
        Direction[] dirs = Direction.values();
        Random rnd = new Random(1);
        // Part A: cost of one tick (driven by hand, tick thread idle at 1 Hz)
        List<Long> times = new ArrayList<>(); int snapBytesMax = 0; long snapBytesSum = 0; int snaps = 0; int played = 0;
        for (int run = 0; run < 20 && played < 20000; run++) {
            GameRoom r = newRoom(players, 1);
            Thread.sleep(50);
            List<ClientSession> ps = r.getPlayers();
            for (int t = 0; t < 2000; t++) {
                for (ClientSession p : ps) {
                    if (rnd.nextInt(10) == 0) r.submitMove(p.getPlayerName(), dirs[rnd.nextInt(dirs.length)]);
                    if (rnd.nextInt(15) == 0) r.submitShoot(p.getPlayerName());
                }
                long t0 = System.nanoTime();
                tick.invoke(r);
                long dt = System.nanoTime() - t0;
                if (r.getState().name().equals("PLAYING")) { times.add(dt); played++; }
                else break;
                if (t % 5 == 0) { int n = MessageCodec.encode(r.buildBinarySnapshot()).length; snapBytesSum += n; snapBytesMax = Math.max(snapBytesMax, n); snaps++; }
            }
            r.stopGame();
        }
        // warm-up skip first 2000
        List<Long> w = times.subList(Math.min(2000, times.size()/2), times.size());
        long[] arr = w.stream().mapToLong(x -> x).sorted().toArray();
        double avg = Arrays.stream(arr).average().orElse(0) / 1e6;
        double p99 = arr[(int)(arr.length*0.99)] / 1e6, max = arr[arr.length-1] / 1e6;
        System.out.printf("A: ticks=%d avg=%.3f ms p99=%.3f ms max=%.3f ms | snapshot avg=%d B max=%d B%n", arr.length, avg, p99, max, snapBytesSum/Math.max(1,snaps), snapBytesMax);
        // Part B: real TickLoop at several rates for 10 s
        for (int rate : new int[]{10, 20, 30, 60, 120}) {
            GameRoom r = newRoom(players, rate);
            List<ClientSession> ps = r.getPlayers();
            long start = System.nanoTime(); int t0 = r.getTickCount();
            while (System.nanoTime() - start < 10_000_000_000L) {
                for (ClientSession p : ps) if (rnd.nextInt(3)==0) r.submitMove(p.getPlayerName(), dirs[rnd.nextInt(dirs.length)]);
                Thread.sleep(20);
                if (!r.getState().name().equals("PLAYING")) break;
            }
            double secs = (System.nanoTime() - start) / 1e9; int ticks = r.getTickCount() - t0;
            String st = r.getState().name();
            r.stopGame();
            System.out.printf("B: rate=%d ticks=%d in %.1fs -> %.1f/s state=%s%n", rate, ticks, secs, ticks/secs, st);
        }
        System.exit(0);
