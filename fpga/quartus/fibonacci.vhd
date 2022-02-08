-- SAS DevOps
-- DOCPF Sample Quartus Prime Project
-- Rudimentary Fibonacci Number Generator

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity fibonacci is
    port(
        clk     :  in std_logic;
        rst     :  in std_logic;
        go      :  in std_logic;
        n       :  in std_logic_vector(7 downto 0);

        result  : out std_logic_vector(7 downto 0);
        done    : out std_logic
    );
end fibonacci;

architecture bhv of fibonacci is
    type STATE_TYPE is (START, WAIT_1, INIT, LOOP_COND, LOOP_BODY, OUTPUT_RESULT);
    signal state: STATE_TYPE;
    signal n_reg : unsigned(7 downto 0);
    signal i, x, y: unsigned (7 downto 0);

begin

    process(clk, rst)
    begin
        if(rst = '1') then
            state <= START;
            done <= '0';
            result <= (others => '0');
            n_reg <= to_unsigned(0, n'length);
            i <= to_unsigned(0, i'length);
            x <= to_unsigned(0, x'length);
            y <= to_unsigned(0, y'length);
        elsif (clk = '1' and clk'event) then
            case state is
                when START =>
                    done <= '0';
                    if (go = '0') then
                        state <= WAIT_1;
                    end if;

                when WAIT_1 =>
                    if (go = '1') then
                        done <= '0';
                        state <= INIT;
                    end if;

                when INIT =>
                    n_reg <= unsigned(n);
                    i <= to_unsigned(3, i'length);
                    x <= to_unsigned(1, x'length);
                    y <= to_unsigned(1, y'length);

                    state <= LOOP_COND;

                when LOOP_COND =>
                    if (i <= n_reg) then
                        state <= LOOP_BODY;
                    else
                        state <= OUTPUT_RESULT;
                    end if;

                when LOOP_BODY =>
                    x <= y;
                    y <= x + y; 
                    i <= i + 1;

                    state <= LOOP_COND;

                when OUTPUT_RESULT =>
                    result <= std_logic_vector(y);
                    done <= '1';

                    if (go = '0') then
                        state <= WAIT_1;
                    end if;

                when others => null;
            end case;
        end if;
    end process;
end bhv;