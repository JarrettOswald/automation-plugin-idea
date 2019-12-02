Feature: Calculator

  Scenario Outline: Sum of the two numbers
    Given two numbers <a> and <b>
    When we try to find sum of our numbers
    Then result should be <result>

    Examples:
      | a | b | result  |
      | 3 | 2 | 5       |