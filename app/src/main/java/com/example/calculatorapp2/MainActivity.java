package com.example.calculatorapp2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class MainActivity extends AppCompatActivity {
    private TextView inputDisplay;
    private TextView resultDisplay;
    private StringBuilder input = new StringBuilder();
    private boolean lastResultCalculated = false;
    private String lastExpression = ""; // Store the last expression for backspace functionality

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputDisplay = findViewById(R.id.inputDisplay);
        resultDisplay = findViewById(R.id.resultDisplay);

        int[] buttonIds = {
                R.id.C, R.id.percent, R.id.cancel, R.id.divide,
                R.id.seven, R.id.eight, R.id.nine, R.id.multiply,
                R.id.four, R.id.five, R.id.six, R.id.minus,
                R.id.one, R.id.two, R.id.three, R.id.plus,
                R.id.doubleZero, R.id.zero, R.id.dot, R.id.equals
        };

        View.OnClickListener buttonClickListener = view -> {
            Button b = (Button) view;
            String buttonText = b.getText().toString();

            // If the last action was calculating a result and we're now entering a new number
            // (not an operation), clear everything to start a new calculation
            if (lastResultCalculated &&
                    (Character.isDigit(buttonText.charAt(0)) || buttonText.equals("00") || buttonText.equals("."))) {
                input.setLength(0);
                inputDisplay.setText("");
                resultDisplay.setText("0");
                lastResultCalculated = false;
            } else if (lastResultCalculated) {
                // If we're continuing with an operation on the previous result
                lastResultCalculated = false;
            }

            switch (buttonText) {
                case "C":
                    input.setLength(0);
                    inputDisplay.setText("");
                    resultDisplay.setText("0");
                    lastResultCalculated = false;
                    break;
                case "⌫":
                    if (lastResultCalculated) {
                        // If we just calculated a result, restore the original expression
                        input.setLength(0);
                        input.append(lastExpression);
                        inputDisplay.setText(formatInputForDisplay(lastExpression));
                        lastResultCalculated = false;
                    } else if (input.length() > 0) {
                        // Regular backspace operation
                        input.setLength(input.length() - 1);
                        inputDisplay.setText(formatInputForDisplay(input.toString()));
                        tryCalculateResult();
                    }
                    break;
                case "=":
                    // Save the current expression before calculating
                    lastExpression = input.toString();
                    calculateFinalResult();
                    break;
                case "%":
                    if (input.length() > 0) {
                        // Check if the last character is already an operator or percent
                        char lastChar = input.charAt(input.length() - 1);
                        if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '%') {
                            // Don't add percent if the last character is an operator or percent
                            break;
                        }

                        // Add percent symbol to display input
                        input.append("%");
                        inputDisplay.setText(formatInputForDisplay(input.toString()));

                        // Calculate the percentage result but keep input display with % symbol
                        calculatePercentResult();
                    }
                    break;
                case "×":
                    // Check if we're allowed to add a multiplication operator
                    if (canAddOperator("*")) {
                        input.append("*");
                        inputDisplay.setText(formatInputForDisplay(input.toString()));
                    }
                    break;
                case "÷":
                    // Check if we're allowed to add a division operator
                    if (canAddOperator("/")) {
                        input.append("/");
                        inputDisplay.setText(formatInputForDisplay(input.toString()));
                    }
                    break;
                case "+":
                    // Check if we're allowed to add an addition operator
                    if (canAddOperator("+")) {
                        input.append("+");
                        inputDisplay.setText(formatInputForDisplay(input.toString()));
                    }
                    break;
                case "-":
                    // For minus sign, we need to handle it specially to allow negative numbers
                    if (canAddMinusSign()) {
                        input.append("-");
                        inputDisplay.setText(formatInputForDisplay(input.toString()));
                    }
                    break;
                default:
                    input.append(buttonText);
                    inputDisplay.setText(formatInputForDisplay(input.toString()));
                    tryCalculateResult();
                    break;
            }
        };

        for (int id : buttonIds) {
            Button b = findViewById(id);
            b.setOnClickListener(buttonClickListener);
        }
    }

    private boolean canAddOperator(String operator) {
        if (input.length() == 0) {
            // Allow only minus at the beginning for negative numbers
            return operator.equals("-");
        }

        char lastChar = input.charAt(input.length() - 1);

        // If the last character is an operator, handle replacement
        if (lastChar == '+' || lastChar == '*' || lastChar == '/' || lastChar == '-') {
            // Check if it's a compound operator with negative sign (like "×-")
            if (lastChar == '-' && input.length() > 1) {
                char beforeMinus = input.charAt(input.length() - 2);
                if (beforeMinus == '+' || beforeMinus == '*' || beforeMinus == '/') {
                    // We have a compound operator (like "×-"), delete both characters
                    input.delete(input.length() - 2, input.length());
                    return true;
                }
            }

            // For a simple operator, just delete the last character
            input.deleteCharAt(input.length() - 1);
            return true;
        }

        return true;
    }

    private boolean canAddMinusSign() {
        if (input.length() == 0) {
            // Allow minus at the beginning for negative numbers
            return true;
        }

        char lastChar = input.charAt(input.length() - 1);

        // If the last character is an operator, allow minus (for negative numbers)
        if (lastChar == '+' || lastChar == '*' || lastChar == '/') {
            return true; // Allow minus after operators for negative numbers
        }

        // Don't allow consecutive minus signs
        if (lastChar == '-') {
            return false;
        }

        return true;
    }

    private String formatInputForDisplay(String input) {
        // Replace operators with their display versions
        return input.replace("*", "×").replace("/", "÷");
    }

    private void tryCalculateResult() {
        try {
            // Don't try to calculate if the input ends with an operator
            String inputStr = input.toString();
            if (inputStr.isEmpty()) {
                resultDisplay.setText("0");
                return;
            }

            char lastChar = inputStr.charAt(inputStr.length() - 1);
            if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '.') {
                return;
            }

            // Check if we have a percent in the input
            if (inputStr.contains("%")) {
                // Let the percent handling logic deal with this
                calculatePercentResult();
                return;
            }

            // Check for division by zero
            if (inputStr.contains("/0") || inputStr.contains("/0.0")) {
                // Check if it's an actual division by zero
                boolean isActualDivisionByZero = false;

                // Simple check for direct division by zero
                if (inputStr.endsWith("/0")) {
                    isActualDivisionByZero = true;
                }
                // Check for division by 0.0, 0.00, etc.
                else {
                    int divIndex = inputStr.indexOf("/");
                    while (divIndex != -1 && !isActualDivisionByZero) {
                        if (divIndex < inputStr.length() - 1) {
                            // Extract the denominator
                            StringBuilder denominator = new StringBuilder();
                            int i = divIndex + 1;
                            while (i < inputStr.length() &&
                                    (Character.isDigit(inputStr.charAt(i)) || inputStr.charAt(i) == '.')) {
                                denominator.append(inputStr.charAt(i));
                                i++;
                            }

                            if (!denominator.toString().isEmpty()) {
                                try {
                                    double denom = Double.parseDouble(denominator.toString());
                                    if (denom == 0) {
                                        isActualDivisionByZero = true;
                                    }
                                } catch (NumberFormatException e) {
                                    // Not a valid number
                                }
                            }
                        }
                        divIndex = inputStr.indexOf("/", divIndex + 1);
                    }
                }

                if (isActualDivisionByZero) {
                    resultDisplay.setText("Cannot be divided by 0");
                    return;
                }
            }

            Expression expression = new ExpressionBuilder(inputStr).build();
            double result = expression.evaluate();

            // Format the result
            resultDisplay.setText(formatResult(result));
        } catch (Exception e) {
            // Check if the error was due to division by zero
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.toLowerCase().contains("divide by zero")) {
                resultDisplay.setText("Cannot be divided by 0");
            }
            // For other errors during typing, don't update the result
        }
    }

    private void calculatePercentResult() {
        try {
            // Get the input string with the percent sign
            String inputStr = input.toString();

            // Check if we have a binary operation like "8%6"
            if (inputStr.contains("%") && !inputStr.endsWith("%")) {
                // This is likely a binary percent operation like "8%6"
                // Standard calculator behavior would be to calculate "8 percent of 6" = 0.48

                // Find the % sign
                int percentIndex = inputStr.indexOf("%");
                if (percentIndex >= 0 && percentIndex < inputStr.length() - 1) {
                    String leftOperand = inputStr.substring(0, percentIndex);
                    String rightOperand = inputStr.substring(percentIndex + 1);

                    try {
                        double leftValue = Double.parseDouble(leftOperand);
                        double rightValue = Double.parseDouble(rightOperand);

                        // Calculate left percent of right (typical calculator behavior)
                        double result = (leftValue / 100) * rightValue;
                        resultDisplay.setText(formatResult(result));
                        return;
                    } catch (NumberFormatException e) {
                        // Not simple numbers, continue with standard percent handling
                    }
                }
            }

            // Standard single-operand percent handling
            // If input ends with percent, perform percentage calculation
            if (inputStr.endsWith("%")) {
                String inputWithoutPercent = inputStr.substring(0, inputStr.length() - 1);

                // Find the last operator position
                int lastOperatorIndex = -1;
                for (int i = inputWithoutPercent.length() - 1; i >= 0; i--) {
                    char c = inputWithoutPercent.charAt(i);
                    if (c == '+' || c == '-' || c == '*' || c == '/') {
                        lastOperatorIndex = i;
                        break;
                    }
                }

                if (lastOperatorIndex == -1) {
                    // No operators, just a number with percent
                    try {
                        double value = Double.parseDouble(inputWithoutPercent);
                        value = value / 100; // Convert to decimal
                        resultDisplay.setText(formatResult(value));
                    } catch (NumberFormatException e) {
                        // Not a valid number
                    }
                } else {
                    // We have an expression with an operator and a percent
                    String operand = inputWithoutPercent.substring(lastOperatorIndex + 1);
                    String operator = String.valueOf(inputWithoutPercent.charAt(lastOperatorIndex));
                    String firstPart = inputWithoutPercent.substring(0, lastOperatorIndex);

                    try {
                        double operandValue = Double.parseDouble(operand);
                        double percentValue = operandValue / 100;

                        // For calculations like "500+10%", treat it as "500+(500*0.1)"
                        if (operator.equals("+") || operator.equals("-")) {
                            try {
                                double firstValue = Double.parseDouble(firstPart);
                                double result;

                                if (operator.equals("+")) {
                                    result = firstValue + (firstValue * percentValue);
                                } else { // operator is "-"
                                    result = firstValue - (firstValue * percentValue);
                                }

                                resultDisplay.setText(formatResult(result));
                                return;
                            } catch (NumberFormatException e) {
                                // First part is not a simple number, continue with normal percent calculation
                            }
                        }

                        // For other operators or if first part is not a simple number
                        // Create a modified expression with the percent value
                        String modifiedExpr = firstPart + operator + percentValue;
                        Expression expression = new ExpressionBuilder(modifiedExpr).build();
                        double result = expression.evaluate();
                        resultDisplay.setText(formatResult(result));
                    } catch (Exception e) {
                        // Error in calculation
                    }
                }
            }
        } catch (Exception e) {
            // Leave the current result as is
        }
    }

    private void calculateFinalResult() {
        try {
            if (input.length() > 0) {
                String inputStr = input.toString();
                // Save the original input for backspace functionality
                lastExpression = inputStr;

                // Check if input ends with an operator
                char lastChar = inputStr.charAt(inputStr.length() - 1);
                if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
                    // Don't calculate if ending with an operator, just return without showing error
                    return;
                }

                // Check for division by zero
                if (inputStr.contains("/0") || inputStr.contains("/0.0")) {
                    // Check if it's an actual division by zero (not like 10/0.5)
                    boolean isActualDivisionByZero = false;

                    // Simple check for direct division by zero
                    if (inputStr.contains("/0") &&
                            (inputStr.endsWith("/0") ||
                                    inputStr.contains("/0+") ||
                                    inputStr.contains("/0-") ||
                                    inputStr.contains("/0*") ||
                                    inputStr.contains("/0/"))) {
                        isActualDivisionByZero = true;
                    }

                    // Check for division by 0.0, 0.00, etc.
                    int divIndex = inputStr.indexOf("/");
                    while (divIndex != -1) {
                        if (divIndex < inputStr.length() - 1) {
                            // Extract the denominator
                            StringBuilder denominator = new StringBuilder();
                            int i = divIndex + 1;
                            while (i < inputStr.length() &&
                                    (Character.isDigit(inputStr.charAt(i)) || inputStr.charAt(i) == '.')) {
                                denominator.append(inputStr.charAt(i));
                                i++;
                            }

                            if (!denominator.toString().isEmpty()) {
                                try {
                                    double denom = Double.parseDouble(denominator.toString());
                                    if (denom == 0) {
                                        isActualDivisionByZero = true;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    // Not a valid number
                                }
                            }
                        }
                        divIndex = inputStr.indexOf("/", divIndex + 1);
                    }

                    if (isActualDivisionByZero) {
                        resultDisplay.setText("Cannot be divided by 0");
                        return;
                    }
                }

                // Handle the case where a number is followed directly by a percent sign (e.g., "8%")
                if (inputStr.endsWith("%")) {
                    String valueWithoutPercent = inputStr.substring(0, inputStr.length() - 1);
                    try {
                        double value = Double.parseDouble(valueWithoutPercent);
                        double result = value / 100; // Convert to decimal (8% = 0.08)

                        // Format and display the result
                        String resultText = formatResult(result);
                        resultDisplay.setText(resultText);

                        // Update the input for continued calculations
                        input.setLength(0);
                        input.append(resultText);

                        inputDisplay.setText("");
                        lastResultCalculated = true;
                        return;
                    } catch (NumberFormatException e) {
                        // If not a valid number, continue with other percent handling below
                    }
                }

                // Handle binary percent operation (like 8%6)
                if (inputStr.contains("%") && !inputStr.endsWith("%")) {
                    int percentIndex = inputStr.indexOf("%");
                    if (percentIndex >= 0 && percentIndex < inputStr.length() - 1) {
                        // This looks like "X%Y" format
                        String leftOperand = inputStr.substring(0, percentIndex);
                        String rightOperand = inputStr.substring(percentIndex + 1);

                        try {
                            double leftValue = Double.parseDouble(leftOperand);
                            double rightValue = Double.parseDouble(rightOperand);

                            // Calculate left percent of right (standard calculator behavior)
                            double result = (leftValue / 100) * rightValue;
                            resultDisplay.setText(formatResult(result));
                            input.setLength(0);
                            input.append(result);
                            inputDisplay.setText("");
                            lastResultCalculated = true;
                            return;
                        } catch (NumberFormatException e) {
                            // Not simple numbers, continue with standard percent handling
                        }
                    }
                }

                // Check if we have a percent symbol in the input but not at the end
                // and not in the X%Y format handled above
                if (inputStr.contains("%") && !inputStr.endsWith("%")) {
                    // Replace percent with calculated values
                    String processedInput = processPercentagesForFinalResult(inputStr);
                    Expression expression = new ExpressionBuilder(processedInput).build();
                    double result = expression.evaluate();

                    // Format the result
                    String resultText = formatResult(result);
                    resultDisplay.setText(resultText);
                    input.setLength(0);
                    input.append(result);
                    inputDisplay.setText("");  // Clear the input display
                    lastResultCalculated = true;
                    return;
                }

                // Normal calculation without percent
                Expression expression = new ExpressionBuilder(inputStr).build();
                double result = expression.evaluate();

                // Format the result
                String resultText = formatResult(result);
                resultDisplay.setText(resultText);
                input.setLength(0);
                input.append(result);
                inputDisplay.setText("");
                lastResultCalculated = true;
            }
        } catch (Exception e) {
            // Check if the error was due to division by zero
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.toLowerCase().contains("divide by zero")) {
                resultDisplay.setText("Cannot be divided by 0");
            } else {
                // For other errors, don't show anything
                // resultDisplay.setText("Error");
            }
            input.setLength(0);
            inputDisplay.setText("");
        }
    }

    private String processPercentagesForFinalResult(String inputStr) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentNum = new StringBuilder();
        char lastOperator = ' ';
        double lastOperand = 0;
        boolean hasPercent = false;

        // First pass: extract first number if it exists
        int i = 0;
        while (i < inputStr.length() &&
                (Character.isDigit(inputStr.charAt(i)) || inputStr.charAt(i) == '.')) {
            currentNum.append(inputStr.charAt(i));
            i++;
        }

        // If we have an initial number
        if (currentNum.length() > 0) {
            lastOperand = Double.parseDouble(currentNum.toString());
            result.append(currentNum);
            currentNum.setLength(0);
        }

        // Process the rest of the expression
        for (; i < inputStr.length(); i++) {
            char c = inputStr.charAt(i);

            if (c == '+' || c == '-' || c == '*' || c == '/') {
                // Process any number+percent before this operator
                if (currentNum.length() > 0) {
                    if (hasPercent) {
                        double num = Double.parseDouble(currentNum.toString());
                        if (lastOperator == '+' || lastOperator == '-') {
                            // For +/-, percent is relative to previous operand
                            result.append((num / 100) * lastOperand);
                        } else {
                            // For */, percent is just division by 100
                            result.append(num / 100);
                        }
                    } else {
                        result.append(currentNum);
                    }
                    currentNum.setLength(0);
                    hasPercent = false;
                }

                // Add the operator
                result.append(c);
                lastOperator = c;
            } else if (c == '%') {
                hasPercent = true;
            } else {
                // Must be a digit or decimal point
                currentNum.append(c);
            }
        }

        // Process any remaining number
        if (currentNum.length() > 0) {
            if (hasPercent) {
                double num = Double.parseDouble(currentNum.toString());
                if (lastOperator == '+' || lastOperator == '-') {
                    // For +/-, percent is relative to previous operand
                    result.append((num / 100) * lastOperand);
                } else {
                    // For */, percent is just division by 100
                    result.append(num / 100);
                }
            } else {
                result.append(currentNum);
            }
        }

        return result.toString();
    }

    private String formatResult(double result) {
        // Format the result (remove .0 for whole numbers)
        if (result == (long) result) {
            return String.format("%d", (long) result);
        } else {
            return String.valueOf(result);
        }
    }
}