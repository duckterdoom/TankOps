package tankops.common.engine;

import tankops.common.model.common.Bullet;
import tankops.common.model.environment.Environment;
import tankops.common.model.powerups.PowerUp;
import tankops.common.model.tank.Tank;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private final List<Environment> environments;
    private final List<Tank> tanks;
    private final List<Bullet> bullets;
    private final List<PowerUp> powerUps;
    private boolean isGameOver;

    public GameState(){
        this.environments = new ArrayList<>();
        this.tanks = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.isGameOver = false;
    }

    //Environments
    //in case: need to reset environment in map
    public void setEnvironments(List<Environment> initialEnvironments){
        this.environments.clear();
        this.environments.addAll(initialEnvironments);
    }

    public List<Environment> getEnvironments() {
        return environments;
    }

    //Tanks
    public void addTank(Tank tank){
        this.tanks.add(tank);
    }
    public List<Tank> getTanks() {
        return tanks;
    }

    public Tank getTankById(String playerId){
        for(Tank tank : this.tanks){
            if(tank.getId().equals(playerId)){
                return tank;
            }
        }

        return null;

    }

    //Bullets
    public void addBullet(Bullet bullet){
        this.bullets.add(bullet);
    }
    public List<Bullet> getBullets() {
        return bullets;
    }
    //PowerUps
    public void addPowerUp(PowerUp powerUp){
        this.powerUps.add(powerUp);
    }
    public List<PowerUp> getPowerUps() {
        return powerUps;
    }


    public boolean isGameOver() {
        return isGameOver;
    }
    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }




}
