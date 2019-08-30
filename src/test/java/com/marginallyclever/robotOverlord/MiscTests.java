package com.marginallyclever.robotOverlord;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import com.marginallyclever.convenience.MatrixOperations;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.dhRobot.DHIKSolver;
import com.marginallyclever.robotOverlord.dhRobot.DHKeyframe;
import com.marginallyclever.robotOverlord.dhRobot.DHLink;
import com.marginallyclever.robotOverlord.dhRobot.robots.sixi2.Sixi2;

public class MiscTests {
	@Test
    public void testCompatibleFonts() {
        String s = "\u23EF";
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        System.out.println("Total fonts: \t" + fonts.length);
        int count = 0;
        for (Font font : fonts) {
            if (font.canDisplayUpTo(s) < 0) {
                count++;
                System.out.println(font.getName());
            }
        }
        System.out.println("Compatible fonts: \t" + count);
    }
	
	/**
	 * @see https://www.eecs.yorku.ca/course_archive/2017-18/W/4421/lectures/Inverse%20kinematics%20-%20annotated.pdf
	 */
	@Test
	public void solveLinearEquations() {
		// we have 6 linear equation and six unknowns
		// p0 = a + b*t0 +  c*t0^2 +  d*t0^3 +  e*t0^4 +   f*t0^5
		// pf = a + b*tf +  c*tf^2 +  d*tf^3 +  e*tf^4 +   f*tf^5
		// v0 =     b    + 2c*t0   + 3d*t0^2 + 4e*t0^3 +  5f*t0^4
		// vf =     b    + 2c*tf   + 3d*tf^2 + 4e*tf^3 +  5f*tf^4
		// a0 =          + 2c      + 6d*t0   + 12e*t0^2 + 20f*t0^3
		// af =          + 2c      + 6d*tf   + 12e*tf^2 + 20f*tf^3
		// or expressed as a matrix, Q = M*N
		// [p0]=[ 1 t0  t0^2  t0^3   t0^4   t0^5][a]
		// [pf]=[ 1 tf  tf^2  tf^3   tf^4   tf^5][b]
		// [v0]=[ 0  1 2t0   3t0^2  4t0^3  5t0^4][c]
		// [vf]=[ 0  1 2tf   3tf^2  4tf^3  5tf^4][d]
		// [a0]=[ 0  0 2     6t0   12t0^2 20t0^3][e]
		// [af]=[ 0  0 2     6tf   12tf^2 20tf^3][f]
		// I know Q and M.  I can Q * inverse(M) to get N.
		// then I can solve the original polynomials for any t betwen t0 and tf.
		
		double t0=0,tf=100;
		double p0=0,pf=90;
		double v0=0,vf=0;
		double a0=0,af=0;

		double[] q = new double[6];
		q[0]=p0;
		q[1]=pf;
		q[2]=v0;
		q[3]=vf;
		q[4]=a0;
		q[5]=af;
		
		long start = System.currentTimeMillis();
		
		double[][] m = buildMatrix(t0,tf);
		double[][] mInv=MatrixOperations.invertMatrix(m);
		double[] n = MatrixOperations.multiply(mInv,q);

		long end = System.currentTimeMillis();
		
		double a=n[0];
		double b=n[1];
		double c=n[2];
		double d=n[3];
		double e=n[4];
		double f=n[5];

		System.out.println("time="+(end-start)+"ms");
		//MatrixOperations.printMatrix(m, 1);
		//MatrixOperations.printMatrix(mInv, 1);
		System.out.println("t\tp\tv\ta\t"+a+"\t"+b+"\t"+c+"\t"+d+"\t"+e+"\t"+f);
		for(double t=t0;t<=tf;t++) {
			// p0 = a + b*t0 +  c*t0^2 +  d*t0^3 +   e*t0^4 +   f*t0^5
			// v0 =     b    + 2c*t0   + 3d*t0^2 +  4e*t0^3 +  5f*t0^4
			// a0 =          + 2c      + 6d*t0   + 12e*t0^2 + 20f*t0^3
			double t2=t*t;
			double t3=t*t*t;
			double t4=t*t*t*t;
			double t5=t*t*t*t*t;
			double pt = a * b*t +   c*t2 +   d*t3 +    e*t4 +    f*t5;
			double vt =     b   + 2*c*t  + 3*d*t2 +  4*e*t3 +  5*f*t4;
			double at =         + 2*c    + 6*d*t  + 12*e*t2 + 20*f*t3;
			System.out.println(t+"\t"+pt+"\t"+vt+"\t"+at);
		}
	}
	
	public double[][] buildMatrix(double t0,double tf) {
		double t02 = t0*t0;
		double tf2 = tf*tf;
		double t03 = t02*t0;
		double tf3 = tf2*tf;
		double t04 = t03*t0;
		double tf4 = tf3*tf;
		double t05 = t04*t0;
		double tf5 = tf4*tf;

		double [][] matrix = new double[6][6];

		// [p0]=[ 1 t0  t0^2  t0^3   t0^4   t0^5][a]
		// [pf]=[ 1 tf  tf^2  tf^3   tf^4   tf^5][b]
		// [v0]=[ 0  1 2t0   3t0^2  4t0^3  5t0^4][c]
		// [vf]=[ 0  1 2tf   3tf^2  4tf^3  5tf^4][d]
		// [a0]=[ 0  0 2     6t0   12t0^2 20t0^3][e]
		// [af]=[ 0  0 2     6tf   12tf^2 20tf^3][f]
		matrix[0][0]=1;	matrix[0][1]=t0;	matrix[0][2]=  t02;	matrix[0][3]=  t03;	matrix[0][4]=   t04;	matrix[0][5]=   t05;
		matrix[1][0]=1;	matrix[1][1]=tf;	matrix[1][2]=  tf2;	matrix[1][3]=  tf3;	matrix[1][4]=   tf4;	matrix[1][5]=   tf5;
		matrix[2][0]=0;	matrix[2][1]= 1;	matrix[2][2]=2*t0;	matrix[2][3]=3*t02;	matrix[2][4]= 4*t03;	matrix[2][5]= 5*t04;
		matrix[3][0]=0;	matrix[3][1]= 1;	matrix[3][2]=2*tf;	matrix[3][3]=3*tf2;	matrix[3][4]= 4*tf3;	matrix[3][5]= 5*tf4;
		matrix[4][0]=0;	matrix[4][1]= 0;	matrix[4][2]=2;		matrix[4][3]=6*t0;	matrix[4][4]=12*t02;	matrix[4][5]=20*t03;
		matrix[5][0]=0;	matrix[5][1]= 0;	matrix[5][2]=2;		matrix[5][3]=6*tf;	matrix[5][4]=12*tf2;	matrix[5][5]=20*tf3;
		
		return matrix;
	}
	
	
	static final double ANGLE_STEP_SIZE=30.0000;
	
	/**
	 * Test SHIKSolver_RTTRTR and DHRobot_Sixi2.
	 * 
	 * In theory Inverse Kinematics (IK) can be given a matrix that, if solved for one solution, produces a set of values
	 * that can be fed to Forward Kinematics (FK) which should reproduce the original matrix.
	 * This test confirms that this theory is true for a wide range of valid angle values in the robot arm.
	 * Put another way, we use one set of matrix0=FK(angles), keyframe = IK(m0), m1=FK(keyframe), then confirm m1==m0.
	 * Remember keyframe might not equal angles because IK can produce more than one correct answer for the same matrix.
	 * 
	 * The code does not check for collisions.  
	 * The granularity of the testing is controlled by ANGLE_STEP_SIZE, which has a O^6 effect, so lower it very carefully.
	 */
	//@Test
	public void testFK2IK() {
		System.out.println("testFK2IK()");
		Sixi2 robot = new Sixi2();
		int numLinks = robot.getNumLinks();
		assert(numLinks>0);

		DHIKSolver solver = robot.getSolverIK();
		DHKeyframe keyframe0 = (DHKeyframe)robot.createKeyframe();
		DHKeyframe keyframe1 = (DHKeyframe)robot.createKeyframe();
		Matrix4d m0 = new Matrix4d();
		Matrix4d m1 = new Matrix4d();
		
		// Find the min/max range for each joint
		DHLink link0 = robot.getLink(0);		double bottom0 = link0.rangeMin;		double top0 = link0.rangeMax;
		DHLink link1 = robot.getLink(1);		double bottom1 = link1.rangeMin;		double top1 = link1.rangeMax;
		DHLink link2 = robot.getLink(2);		double bottom2 = link2.rangeMin;		double top2 = link2.rangeMax;
		// link3 does not bend
		DHLink link4 = robot.getLink(4);		double bottom4 = link4.rangeMin;		double top4 = link4.rangeMax;
		DHLink link5 = robot.getLink(5);		double bottom5 = link5.rangeMin;		double top5 = link5.rangeMax;
		DHLink link6 = robot.getLink(6);		double bottom6 = link6.rangeMin;		double top6 = link6.rangeMax;

		int totalTests = 0;
		int totalOneSolutions = 0;
		int totalPasses = 0;
		
		double x,y,z;
		double u=(bottom4+top4)/2;
		double v=(bottom5+top5)/2;
		double w=(bottom6+top6)/2;

		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new FileWriter(new File("c:/Users/Admin/Desktop/test.txt")));
			out.write("x\ty\tz\tu\tv\tw\tJ0\tJ1\tJ2\tJ3\tJ4\tJ5\tResult\n");

			// go through the entire range of motion of the sixi 2 robot arm
			for(x=bottom0;x<top0;x+=ANGLE_STEP_SIZE) {
				keyframe0.fkValues[0]=x;
				for(y=bottom1;y<top1;y+=ANGLE_STEP_SIZE) {
					keyframe0.fkValues[1]=y;
					for(z=bottom2;z<top2;z+=ANGLE_STEP_SIZE) {
						keyframe0.fkValues[2]=z;
						for(u=bottom4;u<top4;u+=ANGLE_STEP_SIZE) {
							keyframe0.fkValues[3]=u;
							for(v=bottom5;v<top5;v+=ANGLE_STEP_SIZE) {
								keyframe0.fkValues[4]=v;
								for(w=bottom6;w<top6;w+=ANGLE_STEP_SIZE) {
									keyframe0.fkValues[5]=w;
									
									++totalTests;
									// use forward kinematics to find the endMatrix of the pose
				            		robot.setRobotPose(keyframe0);
									m0.set(robot.getLiveMatrix());
									// now generate a set of FK values from the endMatrix m0.
									solver.solve(robot, m0, keyframe1);
									if(solver.solutionFlag==DHIKSolver.ONE_SOLUTION) {
										++totalOneSolutions;
										
										// update the robot pose and get the m1 matrix. 
					            		robot.setRobotPose(keyframe1);
					            		m1.set(robot.getLiveMatrix());
					            		
					            		String message = StringHelper.formatDouble(keyframe0.fkValues[0])+"\t"
					            						+StringHelper.formatDouble(keyframe0.fkValues[1])+"\t"
					            						+StringHelper.formatDouble(keyframe0.fkValues[2])+"\t"
				            							+StringHelper.formatDouble(keyframe0.fkValues[3])+"\t"
				            							+StringHelper.formatDouble(keyframe0.fkValues[4])+"\t"
				            							+StringHelper.formatDouble(keyframe0.fkValues[5])+"\t"
				            							+StringHelper.formatDouble(keyframe1.fkValues[0])+"\t"
					            						+StringHelper.formatDouble(keyframe1.fkValues[1])+"\t"
					            						+StringHelper.formatDouble(keyframe1.fkValues[2])+"\t"
				            							+StringHelper.formatDouble(keyframe1.fkValues[3])+"\t"
				            							+StringHelper.formatDouble(keyframe1.fkValues[4])+"\t"
				            							+StringHelper.formatDouble(keyframe1.fkValues[5])+"\t";
					            		
					            		String error="";
					            		out.write(message);
					            		boolean bad=false;
					            		// it's possible that different fk values are generated but the final matrix is the same.

					            		// compare the m0 and m1 matrixes, which should be identical.
					            		if(!m1.epsilonEquals(m0, 1e-2)) {
					            			Matrix4d diff = new Matrix4d();
					            			diff.sub(m1,m0);
					            			Vector3d a0 = new Vector3d();
					            			Vector3d a1 = new Vector3d();
					            			m0.get(a0);
					            			m1.get(a1);
					            			a0.sub(a1);
					            			error+="Matrix "+a0.length();
					            			bad=true;
					            		}
					            		out.write(error+"\n");
					            		if(bad==false) {
					            			++totalPasses;
					            		}
									}
								}
							}
						}
						out.flush();
					}
				}
			}
			System.out.println("testFK2IK() total="+totalTests+", one solution="+totalOneSolutions+", passes="+totalPasses);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(out!=null) out.flush();
				if(out!=null) out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Plot points along the workspace boundary for the sixi robot in the XZ plane.
	 */
	//@Test
	public void plotXZ() {
		System.out.println("plotXZ()");
		Sixi2 robot = new Sixi2();
		int numLinks = robot.getNumLinks();
		assert(numLinks>0);

		// Find the min/max range for each joint
		DHLink link0 = robot.getLink(0);  double bottom0 = link0.rangeMin;  double top0 = link0.rangeMax;  double mid0 = (top0+bottom0)/2;
		DHLink link1 = robot.getLink(1);  double bottom1 = link1.rangeMin;  double top1 = link1.rangeMax;  double mid1 = (top1+bottom1)/2;
		DHLink link2 = robot.getLink(2);  double bottom2 = link2.rangeMin;  double top2 = link2.rangeMax;//double mid2 = (top2+bottom2)/2;
		// link3 does not bend
		DHLink link4 = robot.getLink(4);  double bottom4 = link4.rangeMin;//double top4 = link4.rangeMax;  double mid4 = (top4+bottom4)/2;  
		DHLink link5 = robot.getLink(5);  double bottom5 = link5.rangeMin;  double top5 = link5.rangeMax;  double mid5 = (top5+bottom5)/2;  
		DHLink link6 = robot.getLink(6);  double bottom6 = link6.rangeMin;//double top6 = link6.rangeMax;  double mid6 = (top6+bottom6)/2;  

		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new FileWriter(new File("c:/Users/Admin/Desktop/plotxz.csv")));
			out.write("X\tY\tZ\n");

			// go through the entire range of motion of the sixi 2 robot arm
			double ANGLE_STEP_SIZE2=1;
			
			double x=mid0;
			double y=bottom1;
			double z=bottom2;
			double u=bottom4;
			double v=bottom5;
			double w=bottom6;

			for(v=bottom5;v<mid5;v+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // picasso box to middle
			for(y=bottom1;y<mid1;y+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot); // shoulder forward
			// skip j0 to keep things on the XZ plane.
			for(;y<top1;y+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // shoulder forward 
			for(;z<top2;z+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // elbow forward  
			for(;v<top5;v+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // picasso box forward

			for(;y>bottom1;y-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // shoulder back 
			for(;z>bottom2;z-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // elbow back  
			for(;v<bottom5;v-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);  // picasso box back
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(out!=null) out.flush();
				if(out!=null) out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Plot points along the workspace boundary for the sixi robot in the XY plane.
	 */
	//@Test
	public void plotXY() {
		System.out.println("plotXY()");
		Sixi2 robot = new Sixi2();
		int numLinks = robot.getNumLinks();
		assert(numLinks>0);

		// Find the min/max range for each joint
		DHLink link0 = robot.getLink(0);  double bottom0 = link0.rangeMin;  double top0 = link0.rangeMax;//double mid0 = (top0+bottom0)/2;
		DHLink link1 = robot.getLink(1);/*double bottom1 = link1.rangeMin;*/double top1 = link1.rangeMax;//double mid1 = (top1+bottom1)/2;
		DHLink link2 = robot.getLink(2);  double bottom2 = link2.rangeMin;  double top2 = link2.rangeMax;//double mid2 = (top2+bottom2)/2;
		// link3 does not bend
		DHLink link4 = robot.getLink(4);  double bottom4 = link4.rangeMin;  double top4 = link4.rangeMax;  double mid4 = (top4+bottom4)/2;  
		DHLink link5 = robot.getLink(5);  double bottom5 = link5.rangeMin;  double top5 = link5.rangeMax;  double mid5 = (top5+bottom5)/2;  
		DHLink link6 = robot.getLink(6);  double bottom6 = link6.rangeMin;  double top6 = link6.rangeMax;  double mid6 = (top6+bottom6)/2;  

		double ANGLE_STEP_SIZE2=1;
		
		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new FileWriter(new File("c:/Users/Admin/Desktop/plotxy.csv")));
			out.write("X\tY\tZ\n");

			// go through the entire range of motion of the sixi 2 robot arm
			// stretch arm forward as much as possible.
			double x=bottom0;
			double y=top1;
			double z=bottom2;
			double u=mid4;
			double v=mid5;
			double w=mid6;

			for(x=bottom0;x<top0;x+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			for(;z<top2;z+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			//for(;v<top5;v+=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			
			for(x=top0;x>bottom0;x-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			//for(;v>mid5;v-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			for(;z>bottom2;z-=ANGLE_STEP_SIZE2) plot(x,y,z,u,v,w,out,robot);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(out!=null) out.flush();
				if(out!=null) out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Used by plotXY() and plotXZ()
	 * @param x
	 * @param y
	 * @param z
	 * @param u
	 * @param v
	 * @param w
	 * @param out
	 * @param robot
	 * @throws IOException
	 */
	private void plot(double x,double y,double z,double u,double v,double w,BufferedWriter out,Sixi2 robot) throws IOException {
		DHKeyframe keyframe0 = (DHKeyframe)robot.createKeyframe();
		Matrix4d m0 = new Matrix4d();
		
		keyframe0.fkValues[0]=x;
		keyframe0.fkValues[1]=y;
		keyframe0.fkValues[2]=z;
		keyframe0.fkValues[3]=u;
		keyframe0.fkValues[4]=v;
		keyframe0.fkValues[5]=w;
					
		// use forward kinematics to find the endMatrix of the pose
		robot.setRobotPose(keyframe0);
		m0.set(robot.getLiveMatrix());
		
		String message = StringHelper.formatDouble(m0.m03)+"\t"
						+StringHelper.formatDouble(m0.m13)+"\t"
						+StringHelper.formatDouble(m0.m23)+"\n";
   		out.write(message);
	}
	
	/**
	 * Use Forward Kinematics to approximate the Jacobian matrix for Sixi.
	 * https://robotacademy.net.au/lesson/velocity-of-6-joint-robot-arm-translation/
	 * https://robotacademy.net.au/lesson/velocity-of-6-joint-robot-arm-rotation/
	 */
	@Test
	public void approximateJacobianMatrix() {
		System.out.println("approximateJacobianMatrix()");
		Sixi2 robot = new Sixi2();

		// Find the min/max range for each joint
		DHLink link0 = robot.getLink(0);  double bottom0 = link0.rangeMin;  double top0 = link0.rangeMax;  double mid0 = (top0+bottom0)/2;
		DHLink link1 = robot.getLink(1);  double bottom1 = link1.rangeMin;  double top1 = link1.rangeMax;  double mid1 = (top1+bottom1)/2;
		DHLink link2 = robot.getLink(2);  double bottom2 = link2.rangeMin;  double top2 = link2.rangeMax;  double mid2 = (top2+bottom2)/2;
		// link3 does not bend
		DHLink link4 = robot.getLink(4);  double bottom4 = link4.rangeMin;  double top4 = link4.rangeMax;  double mid4 = (top4+bottom4)/2;  
		DHLink link5 = robot.getLink(5);  double bottom5 = link5.rangeMin;  double top5 = link5.rangeMax;  double mid5 = (top5+bottom5)/2;  
		DHLink link6 = robot.getLink(6);  double bottom6 = link6.rangeMin;  double top6 = link6.rangeMax;  double mid6 = (top6+bottom6)/2;  

		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new FileWriter(new File("c:/Users/Admin/Desktop/jacobian.csv")));

			// go through the entire range of motion of the sixi 2 robot arm
			double ANGLE_STEP_SIZE2=0.5;
			double x=mid0+0;
			double y=mid1+0;
			double z=mid2+0;
			double u=mid4+0;
			double v=mid5+30;
			double w=mid6+0;

			double [] anglesA = {x,y,z,u,v,w};
			double [] anglesB = {x,y,z,u,v,w};
			
			double [] jacobian = new double[6*6];
			Matrix4d dm = new Matrix4d();
			int i,j;
			
			for(i=0;i<6;++i) {
				for(j=0;j<6;++j) {
					anglesB[j]=anglesA[j];
				}
				anglesB[i]+=ANGLE_STEP_SIZE2;

				// use anglesA to get the hand matrix
				// use anglesB to get the hand matrix after a tiiiiny adjustment on one axis.
				Matrix4d m0 = computeMatrix(anglesA[0],anglesA[1],anglesA[2],anglesA[3],anglesA[4],anglesA[5],robot);
				Matrix4d m1 = computeMatrix(anglesB[0],anglesB[1],anglesB[2],anglesB[3],anglesB[4],anglesB[5],robot);
				
				// use the finite difference in the two matrixes
				// aka the approximate the rate of change (aka the integral, aka the velocity)
				// in one column of the jacobian matrix at this position.
				dm.m00=(m1.m00-m0.m00)/ANGLE_STEP_SIZE2;
				dm.m01=(m1.m01-m0.m01)/ANGLE_STEP_SIZE2;
				dm.m02=(m1.m02-m0.m02)/ANGLE_STEP_SIZE2;
				dm.m03=(m1.m03-m0.m03)/ANGLE_STEP_SIZE2;
				dm.m10=(m1.m10-m0.m10)/ANGLE_STEP_SIZE2;
				dm.m11=(m1.m11-m0.m11)/ANGLE_STEP_SIZE2;
				dm.m12=(m1.m12-m0.m12)/ANGLE_STEP_SIZE2;
				dm.m13=(m1.m13-m0.m13)/ANGLE_STEP_SIZE2;
				dm.m20=(m1.m20-m0.m20)/ANGLE_STEP_SIZE2;
				dm.m21=(m1.m21-m0.m21)/ANGLE_STEP_SIZE2;
				dm.m22=(m1.m22-m0.m22)/ANGLE_STEP_SIZE2;
				dm.m23=(m1.m23-m0.m23)/ANGLE_STEP_SIZE2;
				dm.m30=(m1.m30-m0.m30)/ANGLE_STEP_SIZE2;
				dm.m31=(m1.m31-m0.m31)/ANGLE_STEP_SIZE2;
				dm.m32=(m1.m32-m0.m32)/ANGLE_STEP_SIZE2;
				dm.m33=(m1.m33-m0.m33)/ANGLE_STEP_SIZE2;
				
				jacobian[i*6+0]=dm.m03;
				jacobian[i*6+1]=dm.m13;
				jacobian[i*6+2]=dm.m23;
				
				// find the rotational part
				dm.m03=0;
				dm.m13=0;
				dm.m23=0;
				dm.transpose();
				//[  0 -Wz  Wy]
				//[ Wz   0 -Wx]
				//[-Wy  Wx   0]
				
				jacobian[i*6+0]=dm.m12;
				jacobian[i*6+1]=dm.m20;
				jacobian[i*6+2]=dm.m01;
			}
			
			for(i=0;i<6;++i) {
				for(j=0;j<6;++j) {
					out.write(jacobian[i*6+j]+"\t");
				}
				out.write("\n");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(out!=null) out.flush();
				if(out!=null) out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private Matrix4d computeMatrix(double x,double y,double z,double u,double v,double w,Sixi2 robot) {
		DHKeyframe keyframe0 = (DHKeyframe)robot.createKeyframe();
		keyframe0.fkValues[0]=x;
		keyframe0.fkValues[1]=y;
		keyframe0.fkValues[2]=z;
		keyframe0.fkValues[3]=u;
		keyframe0.fkValues[4]=v;
		keyframe0.fkValues[5]=w;
		
		// use forward kinematics to find the endMatrix of the pose
		robot.setRobotPose(keyframe0);
		return new Matrix4d(robot.getLiveMatrix());
	}
}
