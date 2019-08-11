package be.etrovub.c3d4java.parameters;

public class Vector3 {

	public Vector3(float x, float y, float z, float w)
    {
        this.X = x;
        this.Y = y;
        this.Z = z;
        this.W = w;
    }
	public Vector3(float x, float y, float z)
	{
		this(x,y,z,0);
	}

    public float X, Y, Z, W;
}
