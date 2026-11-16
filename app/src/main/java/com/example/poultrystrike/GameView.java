package com.example.poultrystrike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(3);
    private final Random random = new Random();
    private final Bot[] bots = new Bot[5];
    private int score = 0, ammo = 12, health = 100;
    private float move = 0, aimDrift = 0;
    private boolean moving = false, firing = false;
    private long lastShot = 0, lastFrame = System.currentTimeMillis();
    private String message = "Тапни по врагам и набирай очки!";
    private long messageUntil = System.currentTimeMillis() + 3000;
    private final RectF tmp = new RectF();

    private static class Bot {
        float x, depth, phase;
        boolean alive = true;
    }

    public GameView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        for (int i = 0; i < bots.length; i++) spawn(bots[i] = new Bot());
    }

    private void spawn(Bot b) {
        b.x = 250 + random.nextFloat() * 780;
        b.depth = .65f + random.nextFloat() * .35f;
        b.phase = random.nextFloat() * 6.28f;
        b.alive = true;
    }

    private void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); p.setStrokeWidth(1); }
    private void rect(Canvas c, float l,float t,float r,float b,int col) { color(col); c.drawRect(l,t,r,b,p); }
    private void round(Canvas c,float l,float t,float r,float b,float rad,int col) { color(col); c.drawRoundRect(l,t,r,b,rad,rad,p); }
    private void circle(Canvas c,float x,float y,float rad,int col) { color(col); c.drawCircle(x,y,rad,p); }
    private void text(Canvas c,String s,float x,float y,float size,int col) { color(col); p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)); p.setTextSize(size); c.drawText(s,x,y,p); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float sx = getWidth()/1280f, sy = getHeight()/720f;
        canvas.save(); canvas.scale(sx,sy);
        long now = System.currentTimeMillis();
        float dt = Math.min(.05f,(now-lastFrame)/1000f); lastFrame=now;
        if (moving) aimDrift += move * dt * 190;
        aimDrift *= .985f;
        drawWorld(canvas, now);
        drawBots(canvas, now);
        drawHud(canvas, now);
        canvas.restore();
        postInvalidateDelayed(16);
    }

    private void drawWorld(Canvas c,long now) {
        rect(c,0,0,1280,430,Color.rgb(102,190,232));
        // distant clouds
        circle(c,170,112,35,Color.WHITE); circle(c,210,105,47,Color.WHITE); circle(c,250,118,30,Color.WHITE);
        circle(c,950,100,29,Color.WHITE); circle(c,985,91,40,Color.WHITE); circle(c,1025,104,28,Color.WHITE);
        // horizon and distant arena buildings
        rect(c,0,330,1280,470,Color.rgb(96,142,101));
        rect(c,70,205,310,425,Color.rgb(186,111,75)); rect(c,75,190,305,218,Color.rgb(112,70,56));
        for(int i=0;i<3;i++) round(c,100+i*65,250,140+i*65,315,5,Color.rgb(80,100,112));
        rect(c,970,235,1200,430,Color.rgb(202,155,76)); rect(c,955,215,1210,246,Color.rgb(128,89,46));
        for(int i=0;i<3;i++) round(c,1000+i*62,275,1040+i*62,335,5,Color.rgb(80,100,112));
        // ground perspective
        rect(c,0,430,1280,720,Color.rgb(190,166,119));
        color(Color.rgb(157,132,91)); p.setStrokeWidth(3);
        for(int i=0;i<8;i++){ float y=445+i*i*4; c.drawLine(0,y,1280,y,p); }
        for(int i=-5;i<=5;i++) c.drawLine(640+i*40,430,640+i*145,720,p);
        // center lane marks
        rect(c,623,475,657,510,Color.rgb(235,218,170)); rect(c,612,555,668,602,Color.rgb(235,218,170));
        // weapon silhouette
        round(c,520,620,760,760,28,Color.rgb(57,64,71));
        round(c,575,580,705,690,16,Color.rgb(82,91,98));
        round(c,600,554,680,625,12,Color.rgb(42,47,52));
        round(c,614,540,666,575,8,Color.rgb(119,129,134));
    }

    private void drawBots(Canvas c,long now) {
        for(Bot b:bots) if(b.alive) {
            float wobble=(float)Math.sin(now/330.0+b.phase)*14;
            float x=b.x-aimDrift+wobble;
            float scale=b.depth;
            float h=190*scale, w=112*scale, ground=470+110*(1-scale);
            float top=ground-h;
            // shadow
            color(Color.argb(55,0,0,0)); c.drawOval(x-w*.55f,ground-8,x+w*.55f,ground+8,p);
            // chicken soldier: body, wings, head, helmet
            round(c,x-w*.39f,top+h*.42f,x+w*.39f,ground-7,30*scale,Color.rgb(247,220,174));
            circle(c,x-w*.48f,top+h*.59f,23*scale,Color.rgb(236,198,143));
            circle(c,x+w*.48f,top+h*.59f,23*scale,Color.rgb(236,198,143));
            circle(c,x,top+h*.30f,42*scale,Color.rgb(255,239,205));
            // helmet
            color(Color.rgb(56,105,83)); c.drawArc(x-47*scale,top+h*.10f,x+47*scale,top+h*.38f,180,180,true,p);
            round(c,x-48*scale,top+h*.25f,x+48*scale,top+h*.33f,7*scale,Color.rgb(41,77,64));
            // comb and beak
            circle(c,x-14*scale,top+h*.10f,10*scale,Color.rgb(220,70,62)); circle(c,x+1*scale,top+h*.08f,10*scale,Color.rgb(220,70,62));
            color(Color.rgb(244,161,47)); p.setStyle(Paint.Style.FILL); c.drawOval(x-12*scale,top+h*.31f,x+18*scale,top+h*.40f,p);
            circle(c,x-15*scale,top+h*.27f,4.5f*scale,Color.rgb(35,35,35)); circle(c,x+17*scale,top+h*.27f,4.5f*scale,Color.rgb(35,35,35));
            // little toy blaster
            round(c,x+20*scale,top+h*.55f,x+78*scale,top+h*.63f,5*scale,Color.rgb(55,67,76));
            round(c,x+60*scale,top+h*.57f,x+83*scale,top+h*.61f,3*scale,Color.rgb(28,35,39));
        }
    }

    private void drawHud(Canvas c,long now) {
        // top panels
        round(c,24,22,260,94,18,Color.argb(205,25,35,42));
        text(c,"POULTRY STRIKE",43,54,25,Color.WHITE);
        text(c,"Очки: "+score,43,81,22,Color.rgb(255,220,91));
        round(c,1000,22,1255,94,18,Color.argb(205,25,35,42));
        text(c,"❤ "+health,1022,61,27,Color.rgb(255,111,111));
        text(c,"ПАТРОНЫ  "+ammo+" / ∞",1018,86,17,Color.WHITE);
        // crosshair
        float cx=640,cy=350;
        color(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
        c.drawCircle(cx,cy,23,p); c.drawLine(cx-38,cy,cx-12,cy,p); c.drawLine(cx+12,cy,cx+38,cy,p); c.drawLine(cx,cy-38,cx,cy-12,p); c.drawLine(cx,cy+12,cx,cy+38,p); p.setStyle(Paint.Style.FILL);
        circle(c,cx,cy,3,Color.rgb(255,75,55));
        // movement pad
        circle(c,130,590,78,Color.argb(80,25,35,42)); circle(c,130+move*38,590,34,Color.argb(180,235,242,245));
        text(c,"ДВИЖЕНИЕ",67,690,15,Color.WHITE);
        // fire and reload buttons
        circle(c,1135,575,80,Color.argb(175,220,70,49)); text(c,"ОГОНЬ",1094,584,22,Color.WHITE);
        round(c,1030,650,1240,701,15,Color.argb(175,25,35,42)); text(c,"ПЕРЕЗАРЯДКА",1042,683,17,Color.WHITE);
        if(now<messageUntil) { round(c,390,112,890,158,17,Color.argb(190,25,35,42)); text(c,message,415,143,22,Color.WHITE); }
        else if(score>=20) { round(c,420,112,860,158,17,Color.argb(200,25,100,60)); text(c,"ПОБЕДА! Отличная стрельба!",445,143,22,Color.WHITE); }
    }

    private void shoot(long now) {
        if(now-lastShot<260) return;
        lastShot=now;
        if(ammo<=0) { message="Нажми ПЕРЕЗАРЯДКА"; messageUntil=now+1000; return; }
        ammo--;
        Bot hit=null; float best=9999;
        for(Bot b:bots) if(b.alive) {
            float x=b.x-aimDrift+(float)Math.sin(now/330.0+b.phase)*14;
            float dist=Math.abs(x-640);
            if(dist<65*b.depth+13 && b.depth>best*-1) { // nearest-looking target by depth
                hit=b; best=-b.depth;
            }
        }
        if(hit!=null) { hit.alive=false; score++; message="Попадание! +1"; messageUntil=now+700; spawn(hit); }
        else { message="Мимо! Наведи прицел на курицу"; messageUntil=now+850; }
        if(ammo==0) { message="Магазин пуст — нажми ПЕРЕЗАРЯДКА"; messageUntil=now+1500; }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX()*1280f/getWidth(), y=e.getY()*720f/getHeight();
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            if(x<300 && y>440) { moving=true; move=Math.max(-1,Math.min(1,(x-130)/70f)); }
            else if(x>1000 && y>635) { ammo=12; message="Перезаряжено!"; messageUntil=System.currentTimeMillis()+700; }
            else if(x>1000 && y>455) { firing=true; shoot(System.currentTimeMillis()); }
            else { firing=true; shoot(System.currentTimeMillis()); }
        } else if(action==MotionEvent.ACTION_MOVE) {
            moving=false; firing=false;
            for(int i=0;i<e.getPointerCount();i++) {
                float px=e.getX(i)*1280f/getWidth(), py=e.getY(i)*720f/getHeight();
                if(px<300 && py>440) { moving=true; move=Math.max(-1,Math.min(1,(px-130)/70f)); }
                else if(px>1000 && py<635) firing=true;
            }
        } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_POINTER_UP) {
            if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) { moving=false; firing=false; move=0; }
        }
        return true;
    }
}
