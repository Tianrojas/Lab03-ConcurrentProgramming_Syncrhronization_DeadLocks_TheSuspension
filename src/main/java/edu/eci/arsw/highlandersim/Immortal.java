package edu.eci.arsw.highlandersim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;

    private int health;

    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());

    private final StatusGame status;
    private volatile boolean isAlive = true;
    private boolean free = true;


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb, StatusGame status) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
        this.status = status;
    }

    public void run() {
        while (isAlive) {
            //Se bloquea el primero, atascando los demás en espera
            synchronized (status){
                if(status.isPause()){
                    try {
                        status.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if(immortalsPopulation.size()==1){
                updateCallback.processReport(this + " is the winner!");
                isAlive = false;
                break;
            }

            if (health <= 0) {
                isAlive = false;
                continue;
            }

            Immortal im;

            int myIndex = immortalsPopulation.indexOf(this);

            int nextFighterIndex = immortalsPopulation.size()>0?r.nextInt(immortalsPopulation.size()):-1;

            //avoid self-fight
            if (nextFighterIndex == myIndex || !immortalsPopulation.get(nextFighterIndex).isAlive|| !immortalsPopulation.get(nextFighterIndex).free) {
                nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
            }

            im = immortalsPopulation.get(nextFighterIndex);
            synchronized (this){
                while (!free) {
                    // El hilo espera hasta que esté disponible
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //Me bloqueo a mi y a mi contendor para que no peliemos en dos batallas al tiempo
                free = false;
                im.free  = false;
            }

            this.fight(im);

            synchronized(this) {
                // Libera el hilo para que otros lo puedan usar
                free = true;
                im.free  = true;
                notify();
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public void fight(Immortal i2) {
        if (i2.getHealth() > 0) {
            List<Immortal> Locks = getImSorted( this, i2);
            // Region Critica
            synchronized (Locks.get(0)){
                synchronized (Locks.get(1)){
                    i2.changeHealth(i2.getHealth() - defaultDamageValue);
                    this.health += defaultDamageValue;
                    if (i2.getHealth()<=0){
                        immortalsPopulation.remove(i2);
                    }
                }
            }
            updateCallback.processReport("Fight: " + this + " vs " + i2+"\n");
        } else {
            isAlive = false;
            updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
        }
    }

    public List<Immortal> getImSorted(Immortal i1, Immortal i2) {
        List<Immortal> imSorted = new ArrayList<>();
        // TOMAR EL ULTIMO CHARACTER, CONVERTIRLO A NUMERO Y COMPARARLOS
        if (Integer.parseInt(i1.getName().replaceAll("\\D", "")) < Integer.parseInt(i2.getName().replaceAll("\\D", ""))) {
            imSorted.add(i1);
            imSorted.add(i2);
        } else {
            imSorted.add(i2);
            imSorted.add(i1);
        }
        return imSorted;
    }

    public void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
