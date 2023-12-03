package com.github.igrishaev;

import com.github.igrishaev.msg.ParameterDescription;
import com.github.igrishaev.msg.Parse;

public record PreparedStatement(
        Parse parse,
        ParameterDescription parameterDescription) {}
