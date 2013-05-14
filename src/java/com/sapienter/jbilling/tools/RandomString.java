package com.sapienter.jbilling.tools;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

public class RandomString {

	private String validChars;
	
	public RandomString(){
		validChars ="abcdefghlmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	}
	public RandomString(String validChars){
		this.validChars =validChars;
	}
	
	
	public String getRandomString(int length){
		
		int maxIndex = validChars.length();
		
		String resultID = "";
		java.util.Random rnd = new java.util.Random(System.currentTimeMillis()*(new java.util.Random().nextInt()));
		for (int j=0;j<length;j++ ) {
			int rndPos = Math.abs(rnd.nextInt() % maxIndex);
			resultID += validChars.charAt(rndPos);
			String number=resultID;
		}

		
		return resultID;
	}
	
	public String getRandomString(){
		return getRandomString(8);
	}
}
