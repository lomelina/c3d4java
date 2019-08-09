package be.etrovub.c3d4java.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import be.etrovub.c3d4java.C3dReader;
import be.etrovub.c3d4java.parameters.Vector3;

class C3dReaderTest {

	@Test
	void test() {
		try {
			C3dReader reader = new C3dReader();
			reader.Open("testC3dFiles/test_bb.c3d");
			String name = reader.GetParameter("SUBJECTS:PLAYER_NAME",String.class);
			System.out.println("Name: " + name);
			System.out.println("Number of captured frames: " + reader.getFramesCount());
			for (int i = 0; i < reader.getFramesCount(); i++ ) {
				Vector3 [] points = reader.ReadFrame();
				System.out.println("Point " + i + ": " + points[0].X + ", " + points[0].Y);
			}
			
		} catch(Exception e) {
			
			
		} 
	}

}
