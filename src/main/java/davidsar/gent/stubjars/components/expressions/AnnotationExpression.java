package davidsar.gent.stubjars.components.expressions;

import davidsar.gent.stubjars.components.JarClass;
import davidsar.gent.stubjars.components.JarType;
import davidsar.gent.stubjars.components.writer.Constants;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public class AnnotationExpression extends Expression implements FormattedExpression {
    private final Class<? extends Annotation> annotationType;
    private final String annotationName;
    private final JarClass declaringClass;
    private TypeExpression typeExpression;
    private Expression annotationValue;

    public AnnotationExpression(JarClass declaringClass, Class<? extends Annotation> annotationType, String annotationName) {
        this.annotationType = annotationType;
        this.annotationName = annotationName;
        this.declaringClass = declaringClass;
    }


    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public List<Expression> children() {
        typeExpression = Expressions.forType(annotationType, JarType.toExpression(annotationType, declaringClass));
        annotationValue = Expressions.of(
            Expressions.of(JarType.toExpression(RetentionPolicy.class, declaringClass)),
            StringExpression.PERIOD,
            Expressions.fromString(annotationName));
        return Arrays.asList(
            StringExpression.AT,
            typeExpression,
            Expressions.asParenthetical(annotationValue)
        );
    }


    @Override
    public Expression getFormattedString() {
        if (typeExpression == null) {
            children();
        }
        return Expressions.fromString(Constants.AT, typeExpression.toString(), "(", annotationValue.toString(), ")");
    }
}
