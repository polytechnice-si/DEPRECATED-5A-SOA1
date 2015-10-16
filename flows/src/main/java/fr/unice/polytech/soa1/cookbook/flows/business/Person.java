package fr.unice.polytech.soa1.cookbook.flows.business;


import java.io.Serializable;

public class Person implements Serializable {

	private String firstName;
	private String lastName;
	private int zipCode;
	private String address;
	private String email;
	private String uid;
	private int income;
	private int assets;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public int getZipCode() {
		return zipCode;
	}

	public void setZipCode(int zipCode) {
		this.zipCode = zipCode;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public int getIncome() {
		return income;
	}

	public void setIncome(int income) {
		this.income = income;
	}

	public int getAssets() {
		return assets;
	}

	public void setAssets(int assets) {
		this.assets = assets;
	}

	@Override
	public String toString() {
		return "Person {" +
				"firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", uid='" + uid + '\'' +
				", income=" + income +
				", assets=" + assets +
				'}';
	}
}
