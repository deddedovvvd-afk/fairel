package com.example.poultrystrike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();
    private final Bot[] bots = new Bot[7];
    private final float[] px = new float[8], py = new float[8];
    private float playerX=0, playerZ=0, yaw=0, pitch=0;
    private float joyX=0, joyY=0;
    private int joyPointer=-1, aimPointer=-1, firePointer=-1;
    private float lastAimX, lastAimY;
    private boolean firing=false;
    private int weapon=1, score=0, ammo=30;
    private long lastShot=0, messageUntil=0, lastFrame=System.currentTimeMillis();
    private String message="Слева — движение, справа — веди пальцем прицел";
    private final String[] weaponNames={"ПИСТОЛЕТ","АВТОМАТ","ДРОБОВИК","НОЖ"};

    private static class Bot { float x,z,phase; boolean alive=true; }
    public GameView(Context c) {
        super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        for(int i=0;i<bots.length;i++){ bots[i]=new Bot(); spawn(bots[i]); }
    }
    private void spawn(Bot b){
        b.x=(rng.nextFloat()*2-1)*7f; b.z=9+rng.nextFloat()*18f; b.phase=rng.nextFloat()*6.28f; b.alive=true;
    }
    private void fill(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
    private void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){fill(col);p.setStrokeWidth(w);c.drawLine(x1,y1,x2,y2,p);}
    private void round(Canvas c,float l,float t,float r,float b,float rad,int col){fill(col);c.drawRoundRect(l,t,r,b,rad,rad,p);}
    private void circle(Canvas c,float x,float y,float r,int col){fill(col);c.drawCircle(x,y,r,p);}
    private void text(Canvas c,String s,float x,float y,float size,int col){fill(col);p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));p.setTextSize(size);c.drawText(s,x,y,p);}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas); float sx=getWidth()/1280f, sy=getHeight()/720f; canvas.save();canvas.scale(sx,sy);
        long now=System.currentTimeMillis();float dt=Math.min(.05f,(now-lastFrame)/1000f);lastFrame=now;
        update(dt,now); drawScene(canvas,now);drawBots(canvas,now);drawWeapon(canvas);drawHud(canvas,now);
        canvas.restore();postInvalidateDelayed(16);
    }
    private void update(float dt,long now){
        float forward=-joyY, strafe=joyX; float speed=5.0f;
        playerX+=(strafe*(float)Math.cos(yaw)+forward*(float)Math.sin(yaw))*speed*dt;
        playerZ+=(forward*(float)Math.cos(yaw)-strafe*(float)Math.sin(yaw))*speed*dt;
        playerX=Math.max(-10,Math.min(10,playerX));playerZ=Math.max(-3,Math.min(5,playerZ));
        if(firing && now-lastShot>(weapon==1?115:weapon==0?330:weapon==2?650:480)) attack(now);
    }

    // Perspective projection of world coordinates into the first-person camera.
    private boolean project(float x,float y,float z,float[] out){
        float dx=x-playerX,dz=z-playerZ;float sy=(float)Math.sin(yaw),cy=(float)Math.cos(yaw);
        float vx=dx*cy-dz*sy, vz=dx*sy+dz*cy;
        float cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);
        float vy=(y-1.55f)*cp-vz*sp;float depth=(y-1.55f)*sp+vz*cp;
        if(depth<.22f)return false;
        out[0]=640+vx*540/depth;out[1]=350-vy*540/depth;return true;
    }
    private void poly(Canvas c,float[][] pts,int col){
        Path path=new Path();path.moveTo(pts[0][0],pts[0][1]);for(int i=1;i<pts.length;i++)path.lineTo(pts[i][0],pts[i][1]);path.close();fill(col);c.drawPath(path,p);
    }
    private void quadWorld(Canvas c,float x1,float z1,float x2,float z2,float y,int col){
        float[][] a=new float[4][2];float[] q=new float[2];
        if(!project(x1,y,z1,q))return;a[0]=q.clone();if(!project(x2,y,z1,q))return;a[1]=q.clone();if(!project(x2,y,z2,q))return;a[2]=q.clone();if(!project(x1,y,z2,q))return;a[3]=q.clone();poly(c,a,col);
    }
    private void drawScene(Canvas c,long now){
        // sky, distant hills and sunlight
        p.setShader(new android.graphics.LinearGradient(0,0,0,460,Color.rgb(70,160,216),Color.rgb(205,228,238),android.graphics.Shader.TileMode.CLAMP));c.drawRect(0,0,1280,480,p);p.setShader(null);
        circle(c,1040,105,43,Color.rgb(255,226,133));
        poly(c,new float[][]{{0,350},{130,265},{270,345},{430,245},{600,345},{790,255},{990,350},{1120,270},{1280,345},{1280,500},{0,500}},Color.rgb(82,129,111));
        // large 3D floor plane
        quadWorld(c,-18,-6,18,34,0,Color.rgb(158,147,119));
        // tiled perspective arena floor
        for(int z=-4;z<35;z+=2) quadWorld(c,-18,z,18,z+.045f,0,(z/2%2==0)?Color.rgb(174,160,129):Color.rgb(165,153,124));
        for(int x=-18;x<=18;x+=2) quadWorld(c,x,-5,x+.04f,34,.012f,Color.rgb(124,118,99));
        // perimeter walls and colored panels
        box(c,-12,0,12,1.4f,30,Color.rgb(108,132,130),Color.rgb(74,102,101));
        box(c,-12,0,-5,1.4f,30,Color.rgb(112,137,135),Color.rgb(77,103,102));
        box(c,-12,0,12,1.4f,-5,Color.rgb(156,123,87),Color.rgb(110,82,62));
        // chunky 3D cover blocks and crates
        box(c,-6,0,8,2.0f,10,Color.rgb(190,126,67),Color.rgb(123,76,42));
        box(c,4,0,14,1.7f,18,Color.rgb(66,122,103),Color.rgb(43,80,71));
        box(c,-2,0,18,1.2f,23,Color.rgb(177,94,64),Color.rgb(115,61,49));
        box(c,8,0,10,1.4f,6,Color.rgb(158,154,139),Color.rgb(101,105,103));
        // cover panel decoration
        text(c,"ARENA  /  TRAINING",430,100,22,Color.argb(160,255,255,255));
    }
    private void box(Canvas c,float x,float y,float z,float h,float w,int front,int side){
        float[][] v={{x,y,z},{x+w,y,z},{x+w,y+h,z},{x,y+h,z},{x+w,y,z+w},{x+w,y+h,z+w}};float[][] s=new float[v.length][2];
        for(int i=0;i<v.length;i++)if(!project(v[i][0],v[i][1],v[i][2],s[i]))return;
        poly(c,new float[][]{s[0],s[1],s[2],s[3]},front);
        poly(c,new float[][]{s[1],s[4],s[5],s[2]},side);
        // top face gives the blocks a clear 3D look
        poly(c,new float[][]{s[3],s[2],s[5],new float[]{s[3][0]+(s[4][0]-s[0][0]),s[3][1]+(s[4][1]-s[0][1])}},Color.rgb(Math.min(255,Color.red(front)+25),Math.min(255,Color.green(front)+25),Math.min(255,Color.blue(front)+25)));
        // crate seams
        line(c,(s[0][0]+s[1][0])/2,(s[0][1]+s[1][1])/2,(s[3][0]+s[2][0])/2,(s[3][1]+s[2][1])/2,Color.argb(110,255,220,150),2);
    }
    private void drawBots(Canvas c,long now){
        for(Bot b:bots)if(b.alive){
            float wob=(float)Math.sin(now/280.0+b.phase)*.16f;float[] foot=new float[2],head=new float[2],left=new float[2],right=new float[2];
            if(!project(b.x,0,b.z,foot)||!project(b.x,2.05f,b.z,head))continue;
            float h=foot[1]-head[1]; if(h<4||h>900)continue;float w=h*.52f;
            // soft ground shadow
            fill(Color.argb(45,0,0,0));c.drawOval(foot[0]-w*.48f,foot[1]-h*.035f,foot[0]+w*.48f,foot[1]+h*.025f,p);
            // boots, legs, body armor
            round(c,foot[0]-w*.34f,foot[1]-h*.20f,foot[0]-w*.03f,foot[1],h*.05f,Color.rgb(46,54,49));
            round(c,foot[0]+w*.03f,foot[1]-h*.20f,foot[0]+w*.34f,foot[1],h*.05f,Color.rgb(46,54,49));
            round(c,foot[0]-w*.42f,foot[1]-h*.72f,foot[0]+w*.42f,foot[1]-h*.18f,h*.13f,Color.rgb(211,179,126));
            // backpack/arms
            round(c,foot[0]-w*.58f,foot[1]-h*.66f,foot[0]-w*.32f,foot[1]-h*.35f,h*.07f,Color.rgb(49,91,75));
            round(c,foot[0]+w*.31f,foot[1]-h*.65f,foot[0]+w*.56f,foot[1]-h*.39f,h*.07f,Color.rgb(224,190,135));
            // chicken head, helmet, beak and eyes
            circle(c,foot[0],head[1]+h*.22f,w*.30f,Color.rgb(255,235,195));
            round(c,foot[0]-w*.34f,head[1]+h*.08f,foot[0]+w*.34f,head[1]+h*.24f,h*.08f,Color.rgb(46,101,77));
            circle(c,foot[0]-w*.11f,head[1]+h*.03f,w*.09f,Color.rgb(222,70,60));circle(c,foot[0]+w*.02f,head[1]+h*.01f,w*.09f,Color.rgb(222,70,60));
            circle(c,foot[0]-w*.11f,head[1]+h*.21f,Math.max(2,w*.035f),Color.BLACK);circle(c,foot[0]+w*.12f,head[1]+h*.21f,Math.max(2,w*.035f),Color.BLACK);
            fill(Color.rgb(239,159,48));c.drawOval(foot[0]-w*.03f,head[1]+h*.29f,foot[0]+w*.23f,head[1]+h*.39f,p);
            // target indicator when near center of aim
            float aimDist=(float)Math.hypot(foot[0]-640,head[1]+h*.2f-350);
            if(aimDist<65){fill(Color.argb(210,255,68,55));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(foot[0],head[1]+h*.2f,w*.43f,p);p.setStyle(Paint.Style.FILL);}
        }
    }
    private void drawWeapon(Canvas c){
        if(weapon==3){ // first-person knife
            poly(c,new float[][]{{795,720},{885,720},{826,545},{790,510},{778,530}},Color.rgb(186,202,208));
            line(c,790,555,850,697,Color.rgb(245,250,250),5);round(c,825,660,895,720,12,Color.rgb(57,65,70));
        }else{
            int metal=Color.rgb(51,58,64),light=Color.rgb(105,116,123);
            if(weapon==0){round(c,565,625,740,720,18,metal);round(c,612,587,688,650,9,light);round(c,630,566,670,610,5,metal);}
            else if(weapon==1){round(c,500,625,780,720,20,metal);round(c,550,590,755,650,10,light);round(c,590,572,735,603,5,metal);round(c,590,675,635,720,6,Color.rgb(103,76,53));}
            else{round(c,510,625,770,720,22,metal);round(c,550,587,740,650,10,light);round(c,568,567,650,603,7,Color.rgb(70,78,82));round(c,666,567,748,603,7,Color.rgb(70,78,82));}
        }
    }
    private void drawHud(Canvas c,long now){
        round(c,20,18,285,92,16,Color.argb(205,19,28,34));text(c,"POULTRY STRIKE 3D",38,48,22,Color.WHITE);text(c,"ОЧКИ: "+score,38,77,22,Color.rgb(255,219,87));
        round(c,1005,18,1260,92,16,Color.argb(205,19,28,34));text(c,"ЗАРЯД: "+(weapon==3?"∞":ammo),1025,53,21,Color.WHITE);text(c,weaponNames[weapon],1025,79,16,Color.rgb(255,219,87));
        // centered movable-aim reticle
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.WHITE);c.drawCircle(640,350,22,p);c.drawLine(600,350,626,350,p);c.drawLine(654,350,680,350,p);c.drawLine(640,310,640,336,p);c.drawLine(640,364,640,390,p);p.setStyle(Paint.Style.FILL);circle(c,640,350,3,Color.rgb(240,66,53));
        // movement joystick, interactive when pressed
        circle(c,125,588,75,Color.argb(80,20,28,34));circle(c,125+joyX*38,588-joyY*38,31,Color.argb(185,235,240,241));text(c,"ДВИЖЕНИЕ",63,690,15,Color.WHITE);
        // fire and reload
        circle(c,1140,570,72,Color.argb(185,210,65,48));text(c,weapon==3?"УДАР":"ОГОНЬ",1092,578,23,Color.WHITE);
        round(c,1036,650,1243,703,14,Color.argb(190,25,34,40));text(c,"ПЕРЕЗАРЯДКА",1053,683,17,Color.WHITE);
        // quick-select weapon strip
        String[] names={"ПИСТ","АВТО","ДРОБ","НОЖ"};
        for(int i=0;i<4;i++){int x=390+i*126;int col=(weapon==i)?Color.rgb(227,166,55):Color.argb(205,24,35,42);round(c,x,650,x+112,703,13,col);text(c,names[i],x+14,683,17,Color.WHITE);}
        if(now<messageUntil){round(c,340,104,940,148,14,Color.argb(195,20,29,35));text(c,message,360,133,20,Color.WHITE);}
        else if(score>=25){round(c,460,104,820,148,14,Color.argb(210,27,115,68));text(c,"КЛАСС! 25 ПОПАДАНИЙ",480,133,20,Color.WHITE);}
        text(c,"Проведи пальцем по правой части экрана — двигай прицел",352,618,16,Color.argb(220,255,255,255));
    }
    private void attack(long now){
        lastShot=now;
        if(weapon!=3&&ammo<=0){message="ПАТРОНЫ КОНЧИЛИСЬ — ПЕРЕЗАРЯДИСЬ";messageUntil=now+1200;firing=false;return;}
        if(weapon!=3)ammo-=weapon==2?3:1;
        Bot hit=null;float best=999;
        for(Bot b:bots)if(b.alive){float[] q=new float[2];if(!project(b.x,1.2f,b.z,q))continue;float d=(float)Math.hypot(q[0]-640,q[1]-350);float tolerance=weapon==3?80:weapon==2?68:weapon==1?43:34;float range=weapon==3?2.5f:42;
            float dist=(float)Math.hypot(b.x-playerX,b.z-playerZ);if(d<tolerance&&dist<range&&dist<best){best=dist;hit=b;}}
        if(hit!=null){hit.alive=false;score++;message=(weapon==3?"НОЖЕВОЙ УДАР":"ПОПАДАНИЕ")+"  +1";messageUntil=now+650;spawn(hit);}else if(weapon==3){message="Подойди ближе и наведи прицел";messageUntil=now+700;}else{message="Мимо! Веди прицел пальцем";messageUntil=now+600;}
    }
    private void selectWeapon(float x,float y){if(y>=640&&y<=715&&x>=380&&x<890){int i=(int)((x-390)/126);if(i>=0&&i<4){weapon=i;message="Выбрано: "+weaponNames[i];messageUntil=System.currentTimeMillis()+700;}}}
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(), index=e.getActionIndex(), id=e.getPointerId(index);float x=e.getX(index)*1280/getWidth(),y=e.getY(index)*720/getHeight();
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            if(x<270&&y>455){joyPointer=id;setJoy(x,y);}
            else if(x>=380&&x<890&&y>=640){selectWeapon(x,y);}
            else if(x>1010&&y>635){ammo=30;message="ПЕРЕЗАРЯЖЕНО";messageUntil=System.currentTimeMillis()+650;}
            else if(x>1035&&y>460&&y<640){firePointer=id;firing=true;attack(System.currentTimeMillis());}
            else{aimPointer=id;lastAimX=x;lastAimY=y;}
        }else if(action==MotionEvent.ACTION_MOVE){
            for(int i=0;i<e.getPointerCount();i++){int pid=e.getPointerId(i);float tx=e.getX(i)*1280/getWidth(),ty=e.getY(i)*720/getHeight();
                if(pid==joyPointer)setJoy(tx,ty);
                if(pid==aimPointer){float dx=tx-lastAimX,dy=ty-lastAimY; yaw-=dx*.0062f;pitch=Math.max(-.72f,Math.min(.72f,pitch+dy*.005f));lastAimX=tx;lastAimY=ty;}
            }
        }else if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            if(id==joyPointer){joyPointer=-1;joyX=joyY=0;}if(id==aimPointer)aimPointer=-1;if(id==firePointer){firePointer=-1;firing=false;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){joyPointer=aimPointer=firePointer=-1;joyX=joyY=0;firing=false;}
        }
        return true;
    }
    private void setJoy(float x,float y){float dx=(x-125)/70f,dy=(588-y)/70f;float len=(float)Math.hypot(dx,dy);if(len>1){dx/=len;dy/=len;}joyX=dx;joyY=dy;}
}
