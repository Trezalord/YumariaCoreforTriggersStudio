package fr.yumaria.jobs.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpressionEvaluator {
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    public double evaluate(String expression, Map<String, Double> variables, double fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        try {
            String replaced = replaceVariables(expression, variables);
            Parser parser = new Parser(replaced);
            double value = parser.parseExpression();
            parser.skipWhitespace();
            if (!parser.isEnd() || Double.isNaN(value) || Double.isInfinite(value)) {
                return fallback;
            }
            return value;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String replaceVariables(String expression, Map<String, Double> variables) {
        Matcher matcher = PLACEHOLDER.matcher(expression);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Double value = variables.get(matcher.group(1));
            matcher.appendReplacement(buffer, value == null ? "0" : Matcher.quoteReplacement(Double.toString(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parsePower();
                } else if (match('/')) {
                    value /= parsePower();
                } else {
                    return value;
                }
            }
        }

        private double parsePower() {
            double value = parseUnary();
            skipWhitespace();
            if (match('^')) {
                value = Math.pow(value, parsePower());
            }
            return value;
        }

        private double parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                return -parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                double value = parseExpression();
                if (!match(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                return value;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipWhitespace();
            int start = index;
            boolean dotSeen = false;
            while (!isEnd()) {
                char current = input.charAt(index);
                if (Character.isDigit(current)) {
                    index++;
                    continue;
                }
                if (current == '.' && !dotSeen) {
                    dotSeen = true;
                    index++;
                    continue;
                }
                break;
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected number at " + index);
            }
            return Double.parseDouble(input.substring(start, index));
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (isEnd() || input.charAt(index) != expected) {
                return false;
            }
            index++;
            return true;
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean isEnd() {
            return index >= input.length();
        }
    }
}
