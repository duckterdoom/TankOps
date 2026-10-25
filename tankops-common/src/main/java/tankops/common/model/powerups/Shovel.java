package tankops.common.model.powerups;

import tankops.common.engine.GameState;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.BrickWall;
import tankops.common.model.environment.Environment;
import tankops.common.model.environment.SteelWall;
import tankops.common.model.tank.PlayerTank;

import java.util.Iterator;

public class Shovel extends PowerUp {
    public Shovel(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player, GameState state) {
        player.addScore(SCORE_POINTS);
        if (state.getEnvironments() == null) {
            return;
        }

        Iterator<Environment> iterator = state.getEnvironments().iterator();

        // Danh sách các tường mới cần thêm vào sau khi thay thế
        java.util.List<Environment> newEnvironments = new java.util.ArrayList<>();

        while (iterator.hasNext()) {
            Environment env = iterator.next();

            // Nếu là tường gạch và nằm ở khu vực đáy bản đồ (gần vị trí căn cứ)
            if (env instanceof BrickWall) {
                Vector2D pos = env.getPosition();

                System.out.println("BrickWall tại -> x: " + pos.x() + ", y: " + pos.y());

                // Tạm thời để điều kiện bao quát hoặc đúng mốc vùng giữa mà bạn muốn
                if (pos.y() >= 200.0 && pos.y() <= 400.0 && pos.x() >= 200.0 && pos.x() <= 600.0) {
                    iterator.remove();
                    SteelWall steelWall = new SteelWall(env.getId().replace("env", "steel"), pos);
                    newEnvironments.add(steelWall);
                }
            }
        }

        // Thêm các tường thép thay thế vào danh sách môi trường của game
        state.getEnvironments().addAll(newEnvironments);
        System.out.println("[Shovel] Successfully converted  brick walls to steel walls around the base.");

    }
}
