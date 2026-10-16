package tankops.client.state;

import java.util.ArrayList;
import java.util.List;

public class MockGameState {
    public double tankX = 200;
    public double tankY = 200;
    public double bodyAngle = 0;
    public int tankHp = 75;
    public boolean isMoving = false;

    public double enemyX = 500, enemyY = 300;
    public int enemyHp = 80;

    public static class DummyBullet {
        public double x, y, dx, dy;
        public DummyBullet(double x, double y, double angle) {
            this.x = x; this.y = y;
            double speed = 7.0;

            double rad = Math.toRadians(angle);
            this.dx = (Math.sin(rad) * speed);
            this.dy = -(Math.cos(rad) * speed);
        }
        public void update() {x += dx; y += dy;}
    }

    public List<DummyBullet> bullets = new ArrayList<>();


    //0 = empty, 1 = brick, 2 = steel, 3 = ice, 4 = water, 5 = trees
    public int[][] map = {
            {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
            {2, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 2},
            {2, 0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 2, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 0, 2},
            {2, 0, 1, 1, 0, 0, 0, 5, 5, 0, 0, 0, 2, 0, 0, 0, 5, 5, 0, 0, 0, 1, 1, 0, 2},
            {2, 0, 0, 0, 0, 2, 2, 5, 5, 2, 2, 0, 0, 0, 2, 2, 5, 5, 2, 2, 0, 0, 0, 0, 2},
            {2, 0, 4, 4, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 4, 4, 0, 2},
            {2, 0, 4, 4, 0, 2, 0, 1, 1, 0, 2, 0, 0, 0, 2, 0, 1, 1, 0, 2, 0, 4, 4, 0, 2},
            {2, 0, 0, 0, 0, 2, 2, 1, 1, 2, 2, 0, 0, 0, 2, 2, 1, 1, 2, 2, 0, 0, 0, 0, 2},
            {2, 0, 1, 1, 0, 0, 0, 3, 3, 0, 0, 0, 2, 0, 0, 0, 3, 3, 0, 0, 0, 1, 1, 0, 2},
            {2, 0, 1, 1, 0, 1, 1, 3, 3, 1, 1, 0, 2, 0, 1, 1, 3, 3, 1, 1, 0, 1, 1, 0, 2},
            {2, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 2},
            {2, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 2},
            {2, 0, 1, 1, 0, 2, 2, 0, 0, 2, 2, 0, 2, 0, 2, 2, 0, 0, 2, 2, 0, 1, 1, 0, 2},
            {2, 0, 0, 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 0, 0, 2},
            {2, 0, 5, 5, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 5, 5, 0, 2},
            {2, 0, 5, 5, 0, 1, 1, 4, 4, 1, 1, 0, 0, 0, 1, 1, 4, 4, 1, 1, 0, 5, 5, 0, 2},
            {2, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 2},
            {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2}
    };

    private boolean lastShooting = false;

    public void update(InputHandler input) {
        double speed = 2;
        isMoving = false;

        if (input.isUp()) {
            tankY -= speed;
            bodyAngle = 0;
            isMoving = true;
        } else if (input.isDown()) {
            tankY += speed;
            bodyAngle = 180;
            isMoving = true;
        } else if (input.isLeft()) {
            tankX -= speed;
            bodyAngle = 270;
            isMoving = true;
        } else if (input.isRight()) {
            tankX += speed;
            bodyAngle = 90;
            isMoving = true;
        }

        if (input.isShooting() && !lastShooting) {
            bullets.add(new DummyBullet(tankX, tankY, bodyAngle));
        }
        lastShooting = input.isShooting();

        for (int i = bullets.size() - 1; i >= 0; i--) {
            DummyBullet b = bullets.get(i);
            b.update();
            if (b.x < 0 || b.x > 800 || b.y < 0 || b.y > 600) bullets.remove(i);
        }
    }
}
