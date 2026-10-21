package net.orevilleport.runparticlesonly;

final class Molang {
   private final String expression;
   private final BedrockXpOrbDefinition.EvalContext context;
   private int index;

   private Molang(String expression, BedrockXpOrbDefinition.EvalContext context) {
      this.expression = expression;
      this.context = context;
   }

   static float evaluate(String expression, BedrockXpOrbDefinition.EvalContext context) {
      try {
         return (float)new Molang(expression, context).sum();
      } catch (RuntimeException ignored) {
         return 0.0F;
      }
   }

   private double sum() {
      double value = this.product();

      while (true) {
         this.skipSpace();
         if (this.consume('+')) {
            value += this.product();
         } else {
            if (!this.consume('-')) {
               return value;
            }

            value -= this.product();
         }
      }
   }

   private double product() {
      double value = this.unary();

      while (true) {
         this.skipSpace();
         if (this.consume('*')) {
            value *= this.unary();
         } else {
            if (!this.consume('/')) {
               return value;
            }

            value /= this.unary();
         }
      }
   }

   private double unary() {
      this.skipSpace();
      if (this.consume('+')) {
         return this.unary();
      } else if (this.consume('-')) {
         return -this.unary();
      } else if (this.consume('(')) {
         double value = this.sum();
         this.require(')');
         return value;
      } else {
         return this.index >= this.expression.length() || !Character.isDigit(this.expression.charAt(this.index)) && this.expression.charAt(this.index) != '.'
            ? this.identifier()
            : this.number();
      }
   }

   private double identifier() {
      int start = this.index;

      while (this.index < this.expression.length()) {
         char character = this.expression.charAt(this.index);
         if (!Character.isLetterOrDigit(character) && character != '_' && character != '.') {
            break;
         }

         this.index++;
      }

      if (start == this.index) {
         throw new IllegalArgumentException("Expected Molang value");
      }

      String name = this.expression.substring(start, this.index);
      this.skipSpace();
      if (!this.consume('(')) {
         return this.context.variable(name);
      }

      if (name.equals("Math.clamp")) {
         double value = this.sum();
         this.require(',');
         double minimum = this.sum();
         this.require(',');
         double maximum = this.sum();
         this.require(')');
         return Math.max(minimum, Math.min(value, maximum));
      }

      double first = this.sum();
      double second = 0.0;
      if (this.consume(',')) {
         second = this.sum();
      }

      this.require(')');

      return switch (name) {
         case "Math.sin" -> Math.sin(Math.toRadians(first));
         case "Math.cos" -> Math.cos(Math.toRadians(first));
         case "Math.abs" -> Math.abs(first);
         case "Math.min" -> Math.min(first, second);
         case "Math.max" -> Math.max(first, second);
         case "Math.pow" -> Math.pow(first, second);
         case "Math.random" -> first + (second - first) * this.context.random();
         default -> 0.0;
      };
   }

   private double number() {
      int start = this.index;
      boolean exponent = false;

      while (this.index < this.expression.length()) {
         char character = this.expression.charAt(this.index);
         if (!Character.isDigit(character) && character != '.') {
            if ((character == 'e' || character == 'E') && !exponent) {
               exponent = true;
               this.index++;
               if (this.index < this.expression.length() && (this.expression.charAt(this.index) == '+' || this.expression.charAt(this.index) == '-')) {
                  this.index++;
               }
            } else if (!Character.isDigit(character)) {
               break;
            }
         } else {
            this.index++;
         }
      }

      return Double.parseDouble(this.expression.substring(start, this.index));
   }

   private void skipSpace() {
      while (this.index < this.expression.length() && Character.isWhitespace(this.expression.charAt(this.index))) {
         this.index++;
      }
   }

   private boolean consume(char expected) {
      this.skipSpace();
      if (this.index < this.expression.length() && this.expression.charAt(this.index) == expected) {
         this.index++;
         return true;
      } else {
         return false;
      }
   }

   private void require(char expected) {
      if (!this.consume(expected)) {
         throw new IllegalArgumentException("Expected " + expected);
      }
   }
}
