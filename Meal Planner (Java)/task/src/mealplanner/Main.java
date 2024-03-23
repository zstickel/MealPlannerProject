package mealplanner;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Main {
  static final String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
  static int mealNumber = 1;
  static int ingredientNumber = 1;
  static Map<String, Meal> mealMap = new HashMap<>();
  static Map<String, Integer> ingredientMap = new HashMap<>();
  static ArrayList<Meal> meals = new ArrayList<>();
  public static void main(String[] args) throws SQLException {
    boolean exit = false;
    String DB_URL = "jdbc:postgresql:meals_db";
    String USER = "postgres";
    String PASS = "1111";
    Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
    connection.setAutoCommit(true);
    Statement statement = connection.createStatement();
    Statement secondStatement = connection.createStatement();
    Statement mealNumberStatement = connection.createStatement();

    //statement.executeUpdate("drop table if exists meals");
    statement.executeUpdate("create table if not exists meals (" +
            "category varchar," +
            "meal varchar," +
            "meal_id integer" +
            ")");

    //statement.executeUpdate("drop table if exists ingredients");
    statement.executeUpdate("create table if not exists ingredients (" +
            "ingredient varchar," +
            "ingredient_id integer," +
            "meal_id integer" +
            ")");

    statement.executeUpdate("create table if not exists plan (" +
            "day varchar," +
            "category varchar," +
            "meal_id integer" +
            ")");

    ResultSet maxMealNumberResults = statement.executeQuery("select max(meal_id) as max_meal from meals");
    if (maxMealNumberResults.next()){
      mealNumber= maxMealNumberResults.getInt("max_meal") +1;
    }

    Scanner scanner = new Scanner(System.in);
    while(!exit){
      System.out.println("What would you like to do (add, show, plan, save, exit)?");
      String input = scanner.nextLine();
      switch (input) {
        case "plan" -> plan(scanner, statement);
        case "add" -> addMeal(scanner, statement);
        case "show" -> showMeals(scanner,statement, secondStatement);
        case "save" -> save(statement, secondStatement,scanner);
        case "exit" -> exit = true;
      }
    }
    createIngredientsList(statement,secondStatement);
    System.out.println("Bye!");
    statement.close();
    connection.close();
    scanner.close();
  }

  static void save(Statement statement, Statement secondStatement, Scanner scanner) throws SQLException{
    ResultSet planResults = statement.executeQuery("SELECT * FROM plan");
    if (!planResults.next()){
      System.out.println("Unable to save. Plan your meals first.");
    }else{
      createIngredientsList(statement, secondStatement);
      System.out.println("Input a filename:");
      String filename = scanner.nextLine().trim();
      try(FileWriter fileWriter = new FileWriter(filename)){
        String shoppingList = getShoppingList();
        if (shoppingList != null && !shoppingList.isEmpty()) { // Check if shopping list is not empty
          fileWriter.write(shoppingList);
          System.out.println("Saved!");
        } else {
          System.out.println("Shopping list is empty. Nothing to save.");
        }
      }catch(IOException e){
        System.out.println(e.getMessage());
      }
    }
  }
  static void plan(Scanner scanner, Statement statement)throws SQLException{

    for(String day : weekdays){
      planMealsForDay(day, scanner, statement);
      System.out.println("Yeah! We planned the meals for " + day +".");
    }
    planPrint(statement);

  }

  static void createIngredientsList(Statement statement, Statement secondStatement) throws SQLException{
    ResultSet mealPlanResults = statement.executeQuery("SELECT * FROM plan");
    while (mealPlanResults.next()){
      ResultSet ingredientResults = secondStatement.executeQuery("SELECT * FROM ingredients WHERE meal_id = " + mealPlanResults.getInt("meal_id"));
      while(ingredientResults.next()){
        String ingredient = ingredientResults.getString("ingredient");
        if (ingredientMap.containsKey(ingredient)){
          ingredientMap.put(ingredient, ingredientMap.get(ingredient) + 1);
        }else{
          ingredientMap.put(ingredient,1);
        }
      }
      ingredientResults.close();
    }
    mealPlanResults.close();
  }

  static void showShoppingList(){
    for(Map.Entry<String, Integer> entry: ingredientMap.entrySet()){
      System.out.println("Ingredient: " + entry.getKey());
      System.out.println("Number needed: " + entry.getValue());
    }
  }
  static String getShoppingList(){
    String shoppingList = "";
    for(Map.Entry<String, Integer> entry: ingredientMap.entrySet()) {
      shoppingList += entry.getKey();
      if (entry.getValue() > 1) {
        shoppingList += " x";
        shoppingList += entry.getValue();
      }
      shoppingList+= "\n";

    }

    return shoppingList;

    }


  static void planPrint(Statement statement)throws SQLException{
    int [] mealsPlanned = new int[21];
    int mealIterator = 0;
    ResultSet planResults = statement.executeQuery("SELECT * FROM plan");
    while (planResults.next()){
      mealsPlanned[mealIterator] = planResults.getInt("meal_id");
      mealIterator++;
    }
    planResults.close();
    mealIterator = 0;
    for (String weekday : weekdays){
      System.out.println(weekday);
      System.out.println("Breakfast: " + getMealNameFromId(mealsPlanned[mealIterator], statement));
      mealIterator++;
      System.out.println("Lunch: " + getMealNameFromId(mealsPlanned[mealIterator], statement));
      mealIterator++;
      System.out.println("Dinner: " + getMealNameFromId(mealsPlanned[mealIterator], statement));
      mealIterator++;
    }
  }

  static void planMealsForDay (String dayOfWeek, Scanner scanner, Statement statement)throws SQLException{
    String[] mealTypes = {"breakfast", "lunch", "dinner"};
    System.out.println(dayOfWeek);
    for (String mealType : mealTypes){
      pickSpecificMeal(mealType, dayOfWeek, scanner, statement);
    }

  }
  static void pickSpecificMeal(String mealType,String dayOfWeek, Scanner scanner, Statement statement)throws SQLException{
    String queryStatement = "SELECT * FROM meals WHERE category = '" + mealType +  "' ORDER BY meal ASC";
    ResultSet mealResults = statement.executeQuery(queryStatement);
    ArrayList<String> mealsAvailable = new ArrayList<>();
    while (mealResults.next()){
      String nextMeal = mealResults.getString("meal");
      System.out.println(nextMeal);
      mealsAvailable.add(nextMeal);
    }
    System.out.println("Choose the "+ mealType+   " for " + dayOfWeek +  " from the list above:");
    mealResults.close();
    Boolean validSelection = false;
    String selectedMeal = "";
    while(!validSelection) {
      selectedMeal = scanner.nextLine();
      if (mealsAvailable.contains(selectedMeal)){
        validSelection = true;
      }else{
        System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
      }
    }
    //need validation step
    int mealID = getMealIDFromName(selectedMeal, statement);
    statement.executeUpdate("INSERT INTO plan (day, category, meal_id) VALUES ('" +dayOfWeek + "', '" + mealType + "', " + mealID + ")" );

  }
  static int getMealIDFromName(String name, Statement statement)throws SQLException{
    ResultSet mealIDResultSet = statement.executeQuery("SELECT meal_id FROM meals WHERE meal = '" + name + "'" );
    int mealID = 0;
    if(mealIDResultSet.next()){
      mealID = mealIDResultSet.getInt("meal_id");
    }
    mealIDResultSet.close();
    return mealID;
  }

  static String getMealNameFromId(int id, Statement statement)throws SQLException{
    ResultSet mealNameResultSet = statement.executeQuery("SELECT meal FROM meals WHERE meal_id = " + id );
    String mealName = "";
    if(mealNameResultSet.next()){
      mealName = mealNameResultSet.getString("meal");
      mealNameResultSet.close();
      return mealName;
    }
    mealNameResultSet.close();
    return "NO MEAL";
  }
  static void showMeals(Scanner scanner, Statement statement, Statement secondStatement)throws SQLException {
    System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
    String categoryInput = scanner.nextLine();
    boolean correctInput = false;
    while(!correctInput){
      if (categoryInput.equals("breakfast") || categoryInput.equals("lunch") || categoryInput.equals("dinner")) {
        correctInput = true;
      }else{
        System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
        categoryInput = scanner.nextLine();
      }
    }
    String query = "select * from meals where category = '" + categoryInput + "'";
    ResultSet mealsResultSet = statement.executeQuery(query );

      if(!mealsResultSet.next()){
        System.out.println("No meals found.");
      }else {
       System.out.println();
        System.out.println("Category: " + mealsResultSet.getString("category"));
        System.out.println();
        System.out.println("Name: " + mealsResultSet.getString("meal"));
        ResultSet ingredientResultSet = secondStatement.executeQuery("select * from ingredients where meal_id = " + mealsResultSet.getInt("meal_id"));
        System.out.println("Ingredients:");
        while (ingredientResultSet.next()) {
          System.out.println(ingredientResultSet.getString("ingredient"));
        }
        while (mealsResultSet.next()) {
          System.out.println();
          System.out.println("Name: " + mealsResultSet.getString("meal"));
          System.out.println("Ingredients:");
          ingredientResultSet = secondStatement.executeQuery("select * from ingredients where meal_id = " + mealsResultSet.getInt("meal_id"));
          while (ingredientResultSet.next()) {
            System.out.println(ingredientResultSet.getString("ingredient"));
          }
        }
        System.out.println();
      }
      mealsResultSet.close();

  }
  static void addMeal(Scanner scanner, Statement statement) throws SQLException  {
    boolean correctInput = false;
    String category = "";
    System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
    while(!correctInput){
      category = scanner.nextLine();
      if (category.equals("breakfast") || category.equals("lunch") || category.equals("dinner")) {
        correctInput = true;
      }else{
        System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
      }
    }
    System.out.println("Input the meal's name:");
    boolean correctName = false;
    String name = "";
    while(!correctName){
      name = scanner.nextLine();
      correctName = checkMealName(name);
      if(!correctName){
        System.out.println("Wrong format. Use letters only!");
      }
    }
    System.out.println("Input the ingredients:");
    boolean noBadIngredients = false;

    String ingredientString = "";
    String [] ingredients = null;
    while(!noBadIngredients){
      ingredientString = scanner.nextLine();
      ingredients = ingredientString.split("\\s*,\\s*");
      if (!ingredientString.endsWith(",") && !ingredientString.endsWith(" ") && checkIngredients(ingredients)) {
        noBadIngredients = true;
      }else{

        System.out.println("Wrong format. Use letters only!");
      }
    }
    Meal meal = new Meal(category, name, ingredients);

    statement.executeUpdate("INSERT INTO meals (category, meal, meal_id) " + "VALUES ('" + category + "', '" + name + "', " + mealNumber + ")");

    for (String ingredient : ingredients){
      statement.executeUpdate("INSERT INTO ingredients (ingredient, ingredient_id, meal_id) " + "VALUES ('" + ingredient + "', " + ingredientNumber + ", " + mealNumber + ")");
      ingredientNumber++;
    }
    //mealMap.put(meal.name, meal);
    mealNumber++;
    meals.add(meal);
    System.out.println("The meal has been added!");
  }

  static boolean checkMealName(String name){
    String regex = "^[a-zA-Z ]+$";
    return name.matches(regex);
  }
  static boolean checkIngredients(String[] ingredients){
    String regex = "^[a-zA-Z ]+$";
    boolean noBadIngredients = true;
    for(String ingredient : ingredients){

      if(ingredient.isEmpty() || !ingredient.matches(regex)){
        noBadIngredients = false;
        break;
      }
    }
    return noBadIngredients;
  }

}



class Meal {
  String category;
  String name;
  String[] ingredients;
  Meal(String category, String name, String[] ingredients){
    this.category = category;
    this.name = name;
    this.ingredients = ingredients;
  }
  public void printMeal(){
    System.out.println();
    System.out.println("Category: " + category);
    System.out.println("Name: " + name);
    System.out.println("Ingredients:");
    for(String ingredient : ingredients){
      System.out.println(ingredient);
    }

  }

}