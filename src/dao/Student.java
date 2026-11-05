package dao;

/** A student the FA picks on the dashboard. The reg number stays out of the combo; the FA knows their class by name. */
public record Student(int id, String regNo, String name) {}
