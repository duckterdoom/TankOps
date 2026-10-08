package tankops.client.state;

import java.util.ArrayList;
import java.util.List;

public class MockGameState {
    public double tankX = 200;
    public double tankY = 200;
    public double bodyAngle = 0;
    public double turretAngle = 0;
    public int tankHp = 75;

    public double enemyX = 500, enemyY = 300;
    public int enemyHp = 80;

    public static class DummyBullet {
        public double x, y, dx, dy;
        public DummyBullet(double x, double y, double angle) {
            this.x = x; this.y = y;
            double rad = Math.toRadians(angle);
            this.dx = (Math.cos(rad) * 8);
            this.dy = (Math.sin(rad) * 8);
        }
        public void update() {x += dx; y += dy;}
    }

    public List<DummyBullet> bullets = new ArrayList<>();


    //0 = Trong, 1 = Tuong gach, 2 = Tuong thep
    public int[][] map = {
        {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
        {2, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2},
        {2, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 2},
        {2, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 2},
        {2, 0, 0, 0, 0, 2, 2, 0, 1, 0, 2, 0, 0, 0, 0, 2},
        {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2}
    };

    private boolean lastShooting = false;

    public void update(InputHandler input) {
        double speed = 2.5;

        if (input.isUp()) {tankY -= speed; bodyAngle = 270;}
        if (input.isDown()) {tankY += speed; bodyAngle = 90;}
        if (input.isLeft()) {tankX -= speed; bodyAngle = 180;}
        if (input.isRight()) {tankX += speed; bodyAngle = 0;}

        double dx = input.getMouseX() - tankX;
        double dy = input.getMouseY() - tankY;
        turretAngle = (double) Math.toDegrees(Math.atan2(dy, dx));

        if (input.isShooting() && !lastShooting) {
            bullets.add(new DummyBullet(tankX, tankY, turretAngle));
        }
        lastShooting = input.isShooting();

        for (int i = bullets.size() - 1; i >= 0; i--) {
            DummyBullet b = bullets.get(i);
            b.update();
            if (b.x < 0 || b.x > 800 || b.y < 0 || b.y > 600) bullets.remove(i);
        }
    }
}
