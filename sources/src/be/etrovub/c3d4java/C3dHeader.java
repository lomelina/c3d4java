package be.etrovub.c3d4java;

//-----------------------------------------------------------------------------
//C3dHeader.cs
//
//Class representing C3D file header and exposing information as properties
//
//ETRO, Vrije Universiteit Brussel
//Copyright (C) 2015 Lubos Omelina. All rights reserved.
//-----------------------------------------------------------------------------

public class C3dHeader {

	private byte [] _data;
 
 protected C3dHeader()
 {
     _data = new byte[512];
      setFirstWord((short)0x5002); //_firstWord = 0x5002;
     setNumberOfPoints((short)21);// _numberOfPoints = 21;
     setFirstSampleNumber((short)1);// _firstSampleNumber = 1;
     setLastSampleNumber((short)1);// _lastSampleNumber = 1;
     setFrameRate((float)30);// _frameRate = 30;
     setAnalogSamplesPerFrame((short)0);// _analogSamplesPerFrame = 0;
     setAnalogChannels((short)0);// _analogChannels = 0;
     setScaleFactor((float)-1);// _scaleFactor = -1f;
     setSupport4CharEventLabels(true);// _support4CharEventLabels = true;
 }

 /*private short _firstWord;   
 private byte _firstParameterBlock;
 private short _numberOfPoints;
 private short _analogChannels;

 private short _firstSampleNumber;
 private short _lastSampleNumber;
 private short _maxInterpolationGaps;
 private float _scaleFactor;
 private short _dataStart;
 private short _analogSamplesPerFrame;
 private float _frameRate;

 private boolean _support4CharEventLabels;*/
 
 public void setFirstWord(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data,0, 2);
 }
 public short getFirstWord(){
 	return BitConverter.toInt16(_data, 0);
 }
 public void setFirstParameterBlock(byte value){
 	_data[0] = value;
 }
 public byte getFirstParameterBlock(){
 	return _data[0];
 }
 public void setNumberOfPoints(short value){
 System.arraycopy(BitConverter.getBytes(value), 0, _data, 2, 2);
 }
 public short getNumberOfPoints(){
 	return BitConverter.toInt16(_data, 2);
 }
 public void setAnalogChannels(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 4, 2);
 }
 public short getAnalogChannels(){
 	return BitConverter.toInt16(_data, 4); 
 }
 public void setFirstSampleNumber(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 6, 2);
 }
 public short getFirstSampleNumber(){
 	return BitConverter.toInt16(_data, 6);
 }
 public void setLastSampleNumber(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 8, 2);
 }
 public short getLastSampleNumber(){
 	return BitConverter.toInt16(_data, 8);
 }
 public void setMaxInterpolationGaps(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 10, 2);
 }
 public short setMaxInterpolationGaps(){
 	return BitConverter.toInt16(_data, 10);
 }
 public void setScaleFactor(float value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data,12, 4);
 }
 public float getScaleFactor(){
 	return BitConverter.toFloat(_data, 12);
 }
 public void setDataStart(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 16, 2);
 }
 public short getDataStart(){
 	return BitConverter.toInt16(_data, 16);
 }
 public void setAnalogSamplesPerFrame(short value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 18, 2);
 }
 public short getAnalogSamplesPerFrame(){
 	return BitConverter.toInt16(_data, 18);
 }
 public void setFrameRate(float value){
 	System.arraycopy(BitConverter.getBytes(value), 0, _data, 20, 4);
 }
 public float getFrameRate(){
 	return BitConverter.toFloat(_data, 20);
 }
 public void setSupport4CharEventLabels(boolean value){
 	System.arraycopy(BitConverter.getBytes(value == true? 12345:0), 0, _data, 149*2, 2);
 }
 public boolean getSupport4CharEventLabels(){
 	return BitConverter.toInt16(_data, 149*2) == 12345;
 }
 
 protected void SetHeader(byte[] headerData)
 {
 	System.arraycopy(headerData, 0, _data,0, 512);
 }

 protected byte [] GetRawData() 
 {
     return _data;
 }
}
