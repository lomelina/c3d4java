package be.etrovub.c3d4java.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import be.etrovub.c3d4java.C3dReader;

class C3dReaderTest {

	@Test
	void test() {
		try {
			C3dReader reader = new C3dReader();
			reader.Open("testC3dFiles/test1.c3d");
			String name = reader.GetParameter("SUBJECTS:PLAYER_NAME",String.class);
			System.out.println(name);
		} catch(Exception e) {
			
			
		} 
	}

}
