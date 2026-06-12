package com.ejemplo.model;

public class Empleado {
    private int empNo;       
    private String firstName; 
    private String lastName;  
    private String gender;    

    // Constructor
    public Empleado(int empNo, String firstName, String lastName, String gender) {
        this.empNo = empNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
    }

    // Getters y Setters
    public int getEmpNo() { return empNo; }
    public void setEmpNo(int empNo) { this.empNo = empNo; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @Override
    public String toString() {
        return "Empleado [ID=" + empNo + ", Nombre=" + firstName + " " + lastName + ", Género=" + gender + "]";
    }
}